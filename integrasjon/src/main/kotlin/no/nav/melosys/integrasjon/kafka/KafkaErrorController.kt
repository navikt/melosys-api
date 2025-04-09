package no.nav.melosys.integrasjon.kafka

import mu.KotlinLogging
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger { }

@Unprotected
@RestController
@RequestMapping("/admin/kafka/errors")
class KafkaErrorController(
    private val skippableKafkaErrorHandler: SkippableKafkaErrorHandler,
    private val kafkaContainerService: KafkaContainerService,
    private val environment: Environment
) {
    private fun getHostName() = environment.getProperty("HOSTNAME") ?: "localhost"

    @GetMapping
    fun listErrors(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(
            mapOf(
                "podName" to getHostName(),
                "errors" to skippableKafkaErrorHandler.failedMessages
            )
        )
    }

    @PostMapping("/{key}/retry")
    fun retryError(@PathVariable key: String): ResponseEntity<String> {
        val failed = skippableKafkaErrorHandler.failedMessages[key]
            ?: return ResponseEntity.notFound().build()
        val offset = failed.offset ?: return ResponseEntity.badRequest().body("Offset is missing")
        val topic = failed.topic

        log.info("Retrying failed message with offset $offset")
        skippableKafkaErrorHandler.failedMessages.remove(key)

        // Check if there are containers for this topic
        val existingContainers = kafkaContainerService.findContainersForTopic(topic)
        val hasRunningContainers = existingContainers.any { it.isRunning }

        // Start containers only if we have stopped containers
        var containersStarted = 0
        if (!hasRunningContainers && existingContainers.isNotEmpty()) {
            containersStarted = kafkaContainerService.ensureContainersRunningForTopic(topic)
        }

        return ResponseEntity.ok(
            when {
                containersStarted > 0 ->
                    "Retrying message at offset $offset for topic $topic (started $containersStarted containers)"

                hasRunningContainers ->
                    "Retrying message at offset $offset for topic $topic (containers already running)"

                else ->
                    "Removed error for message at offset $offset for topic $topic. No containers were found or started, so no further action was taken."
            }
        )
    }

    /**
     * Marks a failed Kafka message to be skipped the next time it's encountered.
     *
     * @param key The unique identifier for the failed message in the format "topic-partition-offset"
     *           (e.g. "teammelosys.skattehendelser.v1-0-19"). This key is used to look up the error details in the
     *           failedMessages map.
     */
    @DeleteMapping("/{key}")
    fun skipError(@PathVariable key: String): ResponseEntity<String> {
        val failedMessage = skippableKafkaErrorHandler.failedMessages[key]
            ?: return ResponseEntity.notFound().build()
        val offset = failedMessage.offset ?: return ResponseEntity.badRequest().body("Offset is missing")
        val topic = failedMessage.topic

        log.info("Processing skip request for key: $key on host: ${getHostName()}")

        // Check if there are containers for this topic
        val existingContainers = kafkaContainerService.findContainersForTopic(topic)
        val hasRunningContainers = existingContainers.any { it.isRunning }

        if (skippableKafkaErrorHandler.markToSkip(key)) {
            log.info("Message marked for skipping: $key")

            // Start containers only if we have stopped containers
            var containersStarted = 0
            if (!hasRunningContainers && existingContainers.isNotEmpty()) {
                containersStarted = kafkaContainerService.ensureContainersRunningForTopic(topic)
            }

            return ResponseEntity.ok(
                when {
                    containersStarted > 0 ->
                        "Message marked to be skipped; will resume from offset ${offset + 1} for topic $topic (started $containersStarted containers) host: ${getHostName()}"

                    hasRunningContainers ->
                        "Message marked to be skipped; will resume from offset ${offset + 1} for topic $topic (containers already running) host: ${getHostName()}"

                    else ->
                        "Message marked to be skipped at offset $offset for topic $topic (no containers found or started) host: ${getHostName()}"
                }
            )
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to mark message for skipping")
    }

    @PostMapping("/resume-all")
    fun resumeAllFailedConsumers(): ResponseEntity<String> {
        val failedMessages = skippableKafkaErrorHandler.failedMessages

        if (failedMessages.isEmpty()) {
            return ResponseEntity.ok("No failed messages to resume")
        }

        val messagesByTopic = failedMessages.values.groupBy { it.topic }
        val results = mutableListOf<String>()

        messagesByTopic.forEach { (topic, messages) ->
            messages.forEach { failed ->
                val key = "$topic-${failed.partition}-${failed.offset}"
                skippableKafkaErrorHandler.failedMessages.remove(key)
            }

            val containersStarted = kafkaContainerService.ensureContainersRunningForTopic(topic)

            results.add(
                if (containersStarted > 0)
                    "Topic $topic: cleared ${messages.size} messages, started $containersStarted containers"
                else
                    "Topic $topic: cleared ${messages.size} messages, containers already running"
            )
        }

        return ResponseEntity.ok("Resumed processing:\n" + results.joinToString("\n"))
    }
}
