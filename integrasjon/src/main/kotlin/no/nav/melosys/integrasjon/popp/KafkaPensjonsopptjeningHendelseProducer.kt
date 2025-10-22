package no.nav.melosys.integrasjon.popp

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import no.nav.melosys.config.MDCOperations
import no.nav.melosys.exception.TekniskException
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.env.Environment
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger { }

@Component
class KafkaPensjonsopptjeningHendelseProducer(
    @Value("\${kafka.aiven.popp-hendelser.topic}") private val topicName: String,
    @Qualifier("melosysPoppHendelse") @Autowired private val kafkaTemplate: KafkaTemplate<String, PensjonsopptjeningHendelse>,
    @Autowired private val objectMapper: ObjectMapper,
    private val environment: Environment
) {
    fun sendPensjonsopptjeningHendelse(pensjonsopptjeningHendelse: PensjonsopptjeningHendelse) {
        val hendelseRecord = ProducerRecord<String, PensjonsopptjeningHendelse>(topicName, pensjonsopptjeningHendelse)
        hendelseRecord.headers().add(MDCOperations.CORRELATION_ID, MDCOperations.getCorrelationId().encodeToByteArray())
        val completableFuture = kafkaTemplate.send(hendelseRecord)
        try {
            val resultat = completableFuture[KAFKA_SEND_TIMEOUT_SECONDS, TimeUnit.SECONDS]
            log.info(
                "PensjonsopptjeningHendelse sendt på topic $topicName med offset: ${resultat.recordMetadata.offset()}" +
                    formatHendelseDataForLogging(pensjonsopptjeningHendelse)
            )
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw TekniskException("Avbrutt ved sending av pensjonsopptjeningHendelse $pensjonsopptjeningHendelse", e)
        } catch (e: Exception) {
            throw TekniskException("Kunne ikke sende pensjonsopptjeningHendelse $pensjonsopptjeningHendelse", e)
        }
    }

    private fun formatHendelseDataForLogging(
        pensjonsopptjeningHendelse: PensjonsopptjeningHendelse
    ): String = when {
        environment.activeProfiles.contains("nais") -> ""
        else -> "\ndata: ${pensjonsopptjeningHendelse.toJsonNode().toPrettyString()}"
    }

    private fun Any.toJsonNode(): JsonNode = objectMapper.valueToTree(this)

    companion object {
        private const val KAFKA_SEND_TIMEOUT_SECONDS = 15L
    }
}
