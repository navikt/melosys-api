package no.nav.melosys.integrasjon.kafka

import mu.KotlinLogging
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger { }

@Unprotected
@RestController
@RequestMapping("/admin/kafka/errors")
class KafkaErrorController(
    private val skippableKafkaErrorHandler: SkippableKafkaErrorHandler,
    private val kafkaContainerService: KafkaContainerService
) {

    // List all failed messages
    @GetMapping
    fun listErrors(): Map<String, SkippableKafkaErrorHandler.Failed> = skippableKafkaErrorHandler.failedMessages

    @PostMapping("/{key}/retry")
    fun retryError(@PathVariable key: String): ResponseEntity<String> {
        val failed = skippableKafkaErrorHandler.failedMessages[key]
            ?: return ResponseEntity.notFound().build()
        val offset = failed.offset ?: return ResponseEntity.badRequest().body("Offset is missing")
        val topic = failed.topic

        log.info("Retrying failed message with offset $offset")
        skippableKafkaErrorHandler.failedMessages.remove(key)

        val containersStarted = kafkaContainerService.ensureContainersRunningForTopic(topic)

        return ResponseEntity.ok(
            when {
                containersStarted > 0 -> "Retrying message at offset $offset for topic $topic (started $containersStarted containers)"
                else -> "Retrying message at offset $offset for topic $topic (containers already running)"
            }
        )
    }

    @DeleteMapping("/{key}")
    fun skipError(@PathVariable key: String): ResponseEntity<String> {
        val failed = skippableKafkaErrorHandler.failedMessages[key]
            ?: return ResponseEntity.notFound().build()
        val offset = failed.offset ?: return ResponseEntity.badRequest().body("Offset is missing")
        val topic = failed.topic

        log.info("Processing skip request for key: $key")

        if (skippableKafkaErrorHandler.markToSkip(key)) {
            log.info("Message marked for skipping: $key")

            val containersStarted = kafkaContainerService.ensureContainersRunningForTopic(topic)

            return ResponseEntity.ok(
                when {
                    containersStarted > 0 -> "Message marked to be skipped; will resume from offset ${offset + 1} for topic $topic (started $containersStarted containers)"
                    else -> "Message marked to be skipped; will resume from offset ${offset + 1} for topic $topic (containers already running)"
                }
            )
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to mark message for skipping")
    }

    @PostMapping("/resume")
    fun resumeConsumer(): ResponseEntity<String> {
        val failedTopics = skippableKafkaErrorHandler.failedMessages.values
            .map { it.topic }
            .distinct()

        if (failedTopics.isEmpty()) {
            return ResponseEntity.ok("No failed messages to resume")
        }

        // Only resume containers related to failed topics
        var totalStarted = 0
        val resumedTopics = mutableListOf<String>()

        failedTopics.forEach { topic ->
            val started = kafkaContainerService.ensureContainersRunningForTopic(topic)
            if (started > 0) {
                totalStarted += started
                resumedTopics.add(topic)
            }
        }

        return if (totalStarted > 0) {
            ResponseEntity.ok("Resumed $totalStarted containers for topics: ${resumedTopics.joinToString()}")
        } else {
            ResponseEntity.ok("All containers for failed topics are already running")
        }
    }
}
