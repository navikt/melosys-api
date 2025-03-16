package no.nav.melosys.integrasjon.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.errors.RecordDeserializationException
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger { }

@Service
class KafkaErrorHandler(
    private val objectMapper: ObjectMapper
) : CommonContainerStoppingErrorHandler() {

    val failedMessages = ConcurrentHashMap<String, Failed>()

    override fun handleRemaining(
        thrownException: Exception,
        records: MutableList<ConsumerRecord<*, *>>,
        consumer: Consumer<*, *>,
        container: MessageListenerContainer
    ) {
        val topic = container.containerProperties.topics?.firstOrNull() ?: "unknown"
        val record = records.firstOrNull()
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
        saveError(thrownException, topic, null)
        super.handleOtherException(thrownException, consumer, container, batchListener)
    }

    private fun saveError(thrownException: Exception, topic: String, record: ConsumerRecord<*, *>?) {
        val recordDeserializationException = thrownException as? RecordDeserializationException
        val failedDeserializationException = thrownException.cause as? FailedDeserializationException
        val offset = recordDeserializationException?.offset() ?: record?.offset()
        if (offset == null) log.warn("Fant ikke kafka offset fra Exceptions", thrownException)
        val json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(record?.value())

        val failedMessage = json ?: failedDeserializationException?.rawMessage
        val errorStack = getErrorStack(thrownException)

        val key = "$topic-${record?.partition()}-$offset"
        failedMessages[key] = Failed(
            topic,
            offset,
            record?.partition(),
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
        val record: Map<String, Any?>?, // Store serializable properties instead
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
