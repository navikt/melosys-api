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
class KafkaMelosysHendelseProducer(
    @Value("\${kafka.aiven.melosys-hendelser.topic}") private val topicName: String,
    @Qualifier("melosysHendelse") @Autowired private val kafkaTemplate: KafkaTemplate<String, MelosysHendelse>
) {
    fun produserBestillingsmelding(melosysHendelse: MelosysHendelse) {
        val hendelseRecord = ProducerRecord<String, MelosysHendelse>(topicName, melosysHendelse)
        hendelseRecord.headers().add(MDCOperations.CORRELATION_ID, MDCOperations.getCorrelationId().encodeToByteArray())
        val future = kafkaTemplate.send(hendelseRecord)

        try {
            val sendeResultat = future[15L, TimeUnit.SECONDS]
            log.info(
                "Melding sendt på topic $topicName " +
                    "data: ${melosysHendelse.melding}\n" +
                    "Offset: ${sendeResultat.recordMetadata.offset()} "
            )
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw TekniskException("Avbrutt ved sending av melding om faktura bestilt for hendelse ${melosysHendelse.melding}")
        } catch (e: Exception) {
            throw TekniskException("Kunne ikke sende melding om faktura bestilt for hendelse ${melosysHendelse.melding}", e)
        }
    }
}
