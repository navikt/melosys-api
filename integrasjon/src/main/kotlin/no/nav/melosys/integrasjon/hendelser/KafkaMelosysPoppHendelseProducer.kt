package no.nav.melosys.integrasjon.hendelser

import mu.KotlinLogging
import no.nav.melosys.config.MDCOperations
import no.nav.melosys.exception.TekniskException
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

private val log = KotlinLogging.logger { }

@Component
class KafkaPensjonsopptjeningHendelseProducer(
    @Value("\${kafka.aiven.melosys-popp-hendelser.topic}") private val topicName: String,
    @Qualifier("melosysPoppHendelse") @Autowired private val kafkaTemplate: KafkaTemplate<String, PensjonsopptjeningHendelse>
) {
    fun sendPensjonsopptjeningHendelse(pensjonsopptjeningHendelse: PensjonsopptjeningHendelse) {
        val hendelseRecord = ProducerRecord<String, PensjonsopptjeningHendelse>(topicName, pensjonsopptjeningHendelse)
        hendelseRecord.headers().add(MDCOperations.CORRELATION_ID, MDCOperations.getCorrelationId().encodeToByteArray())
        val future = kafkaTemplate.send(hendelseRecord)

        try {
            val sendeResultat = future[15L, TimeUnit.SECONDS]
            log.info(
                "PensjonsopptjeningHendelse sendt på topic $topicName med offset: ${sendeResultat.recordMetadata.offset()} \n" +
                    "data: $pensjonsopptjeningHendelse"
            )
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw TekniskException("Avbrutt ved sending av pensjonsopptjeningHendelse $pensjonsopptjeningHendelse", e)
        } catch (e: Exception) {
            throw TekniskException("Kunne ikke sende pensjonsopptjeningHendelse $pensjonsopptjeningHendelse", e)
        }
    }
}
