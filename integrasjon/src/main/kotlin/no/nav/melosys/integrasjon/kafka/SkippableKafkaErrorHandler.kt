package no.nav.melosys.integrasjon.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.OffsetAndMetadata
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.RecordDeserializationException
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger { }

@Component
class SkippableKafkaErrorHandler(
    private val objectMapper: ObjectMapper
) : CommonContainerStoppingErrorHandler() {

    val failedMessages = ConcurrentHashMap<String, Failed>()
    private val permanentlySkippedMessages = ConcurrentHashMap<String, Boolean>()

    override fun handleRemaining(
        thrownException: Exception,
        records: MutableList<ConsumerRecord<*, *>>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer
    ) {
        val topic = container.containerProperties.topics?.firstOrNull() ?: "unknown"
        val record = records.firstOrNull()

        // Check if this message has been permanently skipped before
        if (record != null) {
            val key = "${record.topic()}-${record.partition()}-${record.offset()}"
            if (permanentlySkippedMessages[key] == true) {
                log.info("Skipping marked message: $key")
                skipMessage(consumer, record.topic(), record.partition(), record.offset())
                return
            }
        }

        saveError(thrownException, topic, record)
        super.handleRemaining(thrownException, records, consumer, container)
    }

    override fun handleOtherException(
        thrownException: Exception,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer,
        batchListener: Boolean
    ) {
        val topic = container.containerProperties.topics?.firstOrNull() ?: "unknown"
        val recordDeserializationException = thrownException as? RecordDeserializationException

        if (recordDeserializationException != null) {
            val topicPartition = recordDeserializationException.topicPartition()
            val errorTopic = topicPartition.topic()
            val errorPartition = topicPartition.partition()
            val errorOffset = recordDeserializationException.offset()
            val key = "$errorTopic-$errorPartition-$errorOffset"

            saveError(thrownException, errorTopic, null)

            if (permanentlySkippedMessages[key] == true) {
                log.info("Skipping deserialization error: $key")
                skipMessage(consumer, errorTopic, errorPartition, errorOffset)
                return
            }
        } else {
            saveError(thrownException, topic, null)
        }

        super.handleOtherException(thrownException, consumer, container, batchListener)
    }

    private fun skipMessage(consumer: Consumer<*, *>, topic: String, partition: Int, offset: Long) {
        log.info("Skipping failed message: topic=$topic, partition=$partition, offset=$offset")

        try {
            val topicPartition = TopicPartition(topic, partition)
            val offsetAndMetadata = OffsetAndMetadata(offset + 1)
            consumer.commitSync(mapOf(topicPartition to offsetAndMetadata))

            // Directly seek to the next offset to ensure we don't reprocess
            consumer.seek(topicPartition, offset + 1)

            // Clean up
            val key = "$topic-$partition-$offset"
            permanentlySkippedMessages.remove(key)
            failedMessages.remove(key)
        } catch (e: Exception) {
            log.error("Error during message skip operation", e)
        }
    }

    fun markToSkip(key: String): Boolean {
        if (!failedMessages.containsKey(key)) {
            return false
        }

        permanentlySkippedMessages[key] = true
        return true
    }

    private fun saveError(thrownException: Exception, topic: String, record: ConsumerRecord<*, *>?) {
        val recordDeserializationException = thrownException as? RecordDeserializationException
        val failedDeserializationException = thrownException.cause as? FailedDeserializationException

        val (offset, partition) = if (recordDeserializationException != null) {
            val topicPartition = recordDeserializationException.topicPartition()
            recordDeserializationException.offset() to topicPartition.partition()
        } else {
            record?.offset() to record?.partition()
        }

        if (offset == null) log.warn("Fant ikke kafka offset fra Exceptions", thrownException)

        val json = runCatching {
            record?.value()?.let { objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(it) }
        }.getOrNull()

        val key = "$topic-$partition-$offset"
        failedMessages[key] = Failed(
            topic = topic,
            offset = offset,
            partition = partition,
            record = record.toMap(),
            rawMessage = json ?: failedDeserializationException?.rawMessage,
            errorStack = getErrorStack(thrownException)
        )
    }

    private fun getErrorStack(throwable: Throwable?, message: String? = ""): String? {
        if (throwable?.message != null) return getErrorStack(
            throwable.cause,
            "${throwable.message} - (${throwable.javaClass.simpleName})\n$message"
        )
        return message
    }

    data class Failed(
        val topic: String,
        val offset: Long?,
        val partition: Int?,
        val record: Map<String, Any?>?,
        val rawMessage: String?,
        val errorStack: String?
    )

    private fun ConsumerRecord<*, *>?.toMap(): Map<String, Any?>? {
        return this?.let {
            mapOf(
                "key" to it.key(),
                "value" to it.value(),
                "partition" to it.partition(),
                "offset" to it.offset(),
                "timestamp" to it.timestamp()
            )
        }
    }
}
