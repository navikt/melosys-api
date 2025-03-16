package no.nav.melosys

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties

class KafkaOffsetChecker(
    kafkaProperties: KafkaProperties,
    @Value("\${melosys.kafka.consumer.topic}") private val topic: String,
    @Value("\${melosys.kafka.consumer.groupId}") private val groupId: String
) {
    private val topicPartition = TopicPartition(topic, 0)
    private var consumer: KafkaConsumer<String, String> = KafkaConsumer<String, String>(
        kafkaProperties.buildConsumerProperties(null) + mapOf(
            ConsumerConfig.GROUP_ID_CONFIG to groupId
        )
    ).apply { assign(listOf(topicPartition)) }

    private fun getCommittedOffset(): Long {
        val offsetAndMetadata = consumer.committed(setOf(topicPartition))[topicPartition]
        return offsetAndMetadata?.offset() ?: 0
    }

    fun offsetIncreased(block: () -> Unit) : Long {
        val initialOffset = getCommittedOffset()
        block()
        return getCommittedOffset() - initialOffset
    }
}
