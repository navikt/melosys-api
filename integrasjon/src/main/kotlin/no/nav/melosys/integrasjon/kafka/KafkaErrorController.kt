package no.nav.melosys.integrasjon.kafka

import mu.KotlinLogging
import no.nav.melosys.integrasjon.kafka.KafkaErrorHandler.Failed
import no.nav.security.token.support.core.api.Unprotected
import org.springframework.http.ResponseEntity
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.web.bind.annotation.*

private val log = KotlinLogging.logger { }

@Unprotected
@RestController
@RequestMapping("/admin/kafka/errors")
class KafkaErrorController(
    private val kafkaErrorHandler: KafkaErrorHandler,
    private val kafkaListenerService: KafkaListenerService,
    private val registry: KafkaListenerEndpointRegistry
) {

    // List all failed messages
    @GetMapping
    fun listErrors(): Map<String, Failed> = kafkaErrorHandler.failedMessages

    // Retry a failed message (i.e. re-run it)
    @PostMapping("/{key}/retry")
    fun retryError(@PathVariable key: String): ResponseEntity<String> {
        val failed = kafkaErrorHandler.failedMessages[key]
            ?: return ResponseEntity.notFound().build()
        val offset = failed.offset ?: return ResponseEntity.badRequest().body("Offset is missing")

        log.info("Retrying failed message with offset $offset")
        kafkaListenerService.settSpesifiktOffsetPåConsumer(failed.topic, failed.partition!!, offset)
        kafkaErrorHandler.failedMessages.remove(key)

        // Restart consumer so the seek applies
        registry.listenerContainers.forEach { it.start() }

        return ResponseEntity.ok("Retrying message at offset $offset for topic ${failed.topic}")
    }

    @DeleteMapping("/{key}")
    fun skipError(@PathVariable key: String): ResponseEntity<String> {
        val failed = kafkaErrorHandler.failedMessages[key]
            ?: return ResponseEntity.notFound().build()
        val offset = failed.offset ?: return ResponseEntity.badRequest().body("Offset is missing")
        val newOffset = offset + 1

        log.info("Skipping failed message, setting offset to $newOffset")
        kafkaListenerService.settSpesifiktOffsetPåConsumer(failed.topic, failed.partition!!, newOffset)
        kafkaErrorHandler.failedMessages.remove(key)

        // Restart consumer so the seek applies
        registry.listenerContainers.forEach { it.start() }

        return ResponseEntity.ok("Skipped message; now starting at offset $newOffset for topic ${failed.topic}")
    }

    @PostMapping("/resume")
    fun resumeConsumer(): ResponseEntity<String> {
        registry.listenerContainers.forEach { it.start() }
        return ResponseEntity.ok("Consumer container resumed")
    }
}
