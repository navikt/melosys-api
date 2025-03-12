package no.nav.melosys.integrasjon.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.common.errors.RecordDeserializationException
import org.springframework.kafka.listener.CommonContainerStoppingErrorHandler
import org.springframework.kafka.listener.MessageListenerContainer
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Service
class KafkaErrorHandler(
    private val objectMapper: ObjectMapper
) : CommonContainerStoppingErrorHandler() {


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
    }

    fun getErrorStack(throwable: Throwable?, message: String? = ""): String? {
        if (throwable?.message != null) return getErrorStack(
            throwable.cause,
            "${throwable.message} - (${throwable.javaClass.simpleName})\n$message"
        )
        return message
    }
}
