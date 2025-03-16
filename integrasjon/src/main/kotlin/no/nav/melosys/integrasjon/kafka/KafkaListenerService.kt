package no.nav.melosys.integrasjon.kafka

import mu.KotlinLogging
import org.apache.kafka.common.TopicPartition
import org.springframework.kafka.config.KafkaListenerEndpointRegistry
import org.springframework.kafka.listener.AbstractConsumerSeekAware
import org.springframework.kafka.listener.ConsumerSeekAware
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger { }

@Component
class KafkaListenerService(
    private val registry: KafkaListenerEndpointRegistry
) : AbstractConsumerSeekAware() {

    private val pendingSeeks = mutableMapOf<TopicPartition, Long>()

    override fun onPartitionsAssigned(assignments: Map<TopicPartition, Long>, callback: ConsumerSeekAware.ConsumerSeekCallback) {
        assignments.keys.forEach { tp ->
            log.info("Partition assigned: topic=${tp.topic()} partition=${tp.partition()} offset=${assignments[tp]}")

            // Apply pending seeks if any
            pendingSeeks[tp]?.let { offset ->
                log.info("Seeking topic=${tp.topic()} partition=${tp.partition()} to offset=$offset (from pending seeks)")
                callback.seek(tp.topic(), tp.partition(), offset)
                pendingSeeks.remove(tp) // Remove after applying
            }
        }
    }

    fun settSpesifiktOffsetPåConsumer(topic: String, partition: Int, offset: Long) {
        val tp = TopicPartition(topic, partition)
        val container = registry.listenerContainers.firstOrNull()

        if (container == null || !container.isRunning) {
            log.warn("Kafka listener container is not running. Offset seek will be applied on next startup.")
            pendingSeeks[tp] = offset
            return
        }

        log.info("Attempting to seek topic=$topic, partition=$partition to offset=$offset")

        // If consumer is already assigned, seek immediately
        val callback = seekCallbacks[tp]
        if (callback != null) {
            log.info("Seeking topic=$topic, partition=$partition to offset=$offset (immediate)")
            callback.seek(topic, partition, offset)
        } else {
            log.warn("Consumer not yet assigned for topic=$topic, partition=$partition. Storing seek request.")
            pendingSeeks[tp] = offset
        }
    }
}
