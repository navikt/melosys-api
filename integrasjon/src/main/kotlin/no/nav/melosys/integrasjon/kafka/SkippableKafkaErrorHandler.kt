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
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger { }

@Service
class SkippableKafkaErrorHandler(
    private val objectMapper: ObjectMapper
) : CommonContainerStoppingErrorHandler() {

    private val permanentlySkippedMessages = ConcurrentHashMap<String, Boolean>()

    val failedMessages = ConcurrentHashMap<String, Failed>()
    private val messagesToSkip = ConcurrentHashMap<String, Boolean>()

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
                log.info("Skipping previously marked message: $key")
                skipMessage(consumer, record.topic(), record.partition(), record.offset())
                return
            }
        }
        saveError(thrownException, topic, record)
        // Default behavior
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
            val deserTopic = topicPartition.topic()
            val deserPartition = topicPartition.partition()
            val deserOffset = recordDeserializationException.offset()
            val key = "$deserTopic-$deserPartition-$deserOffset"

            // Save error info first
            saveError(thrownException, deserTopic, null)

            // Check if it should be skipped
            if (permanentlySkippedMessages[key] == true || messagesToSkip[key] == true) {
                log.info("Skipping deserialization error: $key")
                skipMessage(consumer, deserTopic, deserPartition, deserOffset)

                // Pause container briefly to break potential loop
                try {
                    container.pause();
                    Thread.sleep(200);
                    container.resume();
                } catch (e: Exception) {
                    log.error("Error pausing/resuming container", e)
                }

                return
            }
        } else {
            // For other types of exceptions, save what we can
            saveError(thrownException, topic, null)
        }

        super.handleOtherException(thrownException, consumer, container, batchListener)
    }

    private fun skipMessage(consumer: Consumer<*, *>, topic: String, partition: Int, offset: Long) {
        log.info("Skipping failed message: topic=$topic, partition=$partition, offset=$offset")

        try {
            // Commit the offset to skip this message
            val topicPartition = TopicPartition(topic, partition)
            val offsetAndMetadata = OffsetAndMetadata(offset + 1)
            consumer.commitSync(mapOf(topicPartition to offsetAndMetadata))

            // IMPORTANT: Directly seek to the next offset to ensure we don't reprocess
            consumer.seek(topicPartition, offset + 1)

            // Clean up
            val key = "$topic-$partition-$offset"
            messagesToSkip.remove(key)
            failedMessages.remove(key)

            // Add a short pause to allow Kafka to process the commit
            Thread.sleep(100)
        } catch (e: Exception) {
            log.error("Error during message skip operation", e)
        }
    }

    fun markToSkip(key: String): Boolean {
        if (!failedMessages.containsKey(key)) {
            return false
        }

        // Mark both for immediate skipping and permanent skipping
        messagesToSkip[key] = true
        permanentlySkippedMessages[key] = true
        return true
    }

    private fun saveError(thrownException: Exception, topic: String, record: ConsumerRecord<*, *>?) {
        val recordDeserializationException = thrownException as? RecordDeserializationException
        val failedDeserializationException = thrownException.cause as? FailedDeserializationException

        // Try to get offset and partition from different sources
        val offset: Long?
        val partition: Int?

        if (recordDeserializationException != null) {
            val topicPartition = recordDeserializationException.topicPartition()
            offset = recordDeserializationException.offset()
            partition = topicPartition.partition()
        } else {
            offset = record?.offset()
            partition = record?.partition()
        }

        if (offset == null) log.warn("Fant ikke kafka offset fra Exceptions", thrownException)

        val json = try {
            record?.value()?.let { objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(it) }
        } catch (e: Exception) {
            null
        }

        val failedMessage = json ?: failedDeserializationException?.rawMessage
        val errorStack = getErrorStack(thrownException)

        val key = "$topic-$partition-$offset"
        failedMessages[key] = Failed(
            topic,
            offset,
            partition,
            record.toMap(),
            failedMessage,
            errorStack
        )
    }

    fun getErrorStack(throwable: Throwable?, message: String? = ""): String? {
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

    fun ConsumerRecord<*, *>?.toMap(): Map<String, Any?>? {
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
