package no.nav.melosys

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.springframework.boot.autoconfigure.kafka.KafkaProperties

class KafkaOffsetChecker(
    private val kafkaProperties: KafkaProperties
) {

    private fun getCommittedOffset(topic: String, groupId: String): Long {
        val topicPartition = TopicPartition(topic, 0)
        val consumer: KafkaConsumer<String, String> = KafkaConsumer<String, String>(
            kafkaProperties.buildConsumerProperties(null) + mapOf(
                ConsumerConfig.GROUP_ID_CONFIG to groupId
            )
        ).apply { assign(listOf(topicPartition)) }

        val offsetAndMetadata = consumer.committed(setOf(topicPartition))[topicPartition]
        return offsetAndMetadata?.offset() ?: 0
    }

    fun offsetIncreased(topic: String, groupId: String, block: () -> Unit): Long {
        val initialOffset = getCommittedOffset(topic, groupId)
        block()
        return getCommittedOffset(topic, groupId) - initialOffset
    }
}
