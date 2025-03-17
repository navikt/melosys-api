package no.nav.melosys.integrasjon.kafka

import mu.KotlinLogging
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger { }

@Unprotected
@RestController
@RequestMapping("/admin/kafka/errors")
class KafkaErrorController(
    private val skippableKafkaErrorHandler: SkippableKafkaErrorHandler,
    private val registry: KafkaListenerEndpointRegistry
) {

    // List all failed messages
    @GetMapping
    fun listErrors(): Map<String, SkippableKafkaErrorHandler.Failed> = skippableKafkaErrorHandler.failedMessages

    // Retry a failed message (i.e. re-run it)
    @PostMapping("/{key}/retry")
    fun retryError(@PathVariable key: String): ResponseEntity<String> {
        val failed = skippableKafkaErrorHandler.failedMessages[key]
            ?: return ResponseEntity.notFound().build()
        val offset = failed.offset ?: return ResponseEntity.badRequest().body("Offset is missing")

        log.info("Retrying failed message with offset $offset")
        // Remove from failed messages so it's processed normally
        skippableKafkaErrorHandler.failedMessages.remove(key)

        // Restart consumer - it will automatically resume from where it stopped
        registry.listenerContainers.forEach { it.start() }

        return ResponseEntity.ok("Retrying message at offset $offset for topic ${failed.topic}")
    }

    @DeleteMapping("/{key}")
    fun skipError(@PathVariable key: String): ResponseEntity<String> {
        val failed = skippableKafkaErrorHandler.failedMessages[key]
            ?: return ResponseEntity.notFound().build()
        val offset = failed.offset ?: return ResponseEntity.badRequest().body("Offset is missing")

        log.info("Processing skip request for key: $key")

        // Mark the message to be skipped
        if (skippableKafkaErrorHandler.markToSkip(key)) {
            log.info("Message marked for skipping: $key")

            // Restart the containers
            registry.listenerContainers.forEach {
                log.info("Starting container: ${it.listenerId}")
                it.start()
            }

            return ResponseEntity.ok("Message marked to be skipped; will resume from offset ${offset + 1} for topic ${failed.topic}")
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to mark message for skipping")
    }

    @PostMapping("/resume")
    fun resumeConsumer(): ResponseEntity<String> {
        registry.listenerContainers.forEach { it.start() }
        return ResponseEntity.ok("Consumer container resumed")
    }
}
