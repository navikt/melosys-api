package no.nav.melosys.integrasjon.kafka

import mu.KotlinLogging
import no.nav.melosys.integrasjon.kafka.SkippableKafkaErrorHandler.Failed
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
    private val kafkaListenerService: KafkaListenerService,
    private val registry: KafkaListenerEndpointRegistry,
    private val kafkaErrorHandler : SkippableKafkaErrorHandler
) {

    // List all failed messages
    @GetMapping
    fun listErrors(): Map<String, Failed> = skippableKafkaErrorHandler.failedMessages

    // Retry a failed message (i.e. re-run it)
    @PostMapping("/{key}/retry")
    fun retryError(@PathVariable key: String): ResponseEntity<String> {
        val failed = skippableKafkaErrorHandler.failedMessages[key]
            ?: return ResponseEntity.notFound().build()
        val offset = failed.offset ?: return ResponseEntity.badRequest().body("Offset is missing")

        log.info("Retrying failed message with offset $offset")
        kafkaListenerService.settSpesifiktOffsetPåConsumer(failed.topic, failed.partition!!, offset)
        skippableKafkaErrorHandler.failedMessages.remove(key)

        // Restart consumer so the seek applies
        registry.listenerContainers.forEach { it.start() }

        return ResponseEntity.ok("Retrying message at offset $offset for topic ${failed.topic}")
    }

    @DeleteMapping("/{key}")
    fun skipError(@PathVariable key: String): ResponseEntity<String> {
        val failed = kafkaErrorHandler.failedMessages[key]
            ?: return ResponseEntity.notFound().build()
        val offset = failed.offset ?: return ResponseEntity.badRequest().body("Offset is missing")

        // Mark the message to be skipped
        if (kafkaErrorHandler.markToSkip(key)) {
            // Set the consumer position to the message that caused the error
            kafkaListenerService.settSpesifiktOffsetPåConsumer(failed.topic, failed.partition!!, offset)

            // Restart consumer so the seek applies
            registry.listenerContainers.forEach { it.start() }

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
