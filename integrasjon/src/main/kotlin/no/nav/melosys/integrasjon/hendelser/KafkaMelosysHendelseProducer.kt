package no.nav.melosys.integrasjon.hendelser

import mu.KotlinLogging
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
        val beskrivelse = melosysHendelse.melding.beskrivelseUtenPersonopplysninger()

        try {
            val sendeResultat = kafkaTemplate.send(hendelseRecord)[15L, TimeUnit.SECONDS]
            log.info("Melding sendt på topic $topicName: $beskrivelse. Offset: ${sendeResultat.recordMetadata.offset()}")
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw TekniskException("Avbrutt ved sending av melding på topic $topicName: $beskrivelse")
        } catch (e: Exception) {
            throw TekniskException("Kunne ikke sende melding på topic $topicName: $beskrivelse", e)
        }
    }

    private fun HendelseMelding.beskrivelseUtenPersonopplysninger(): String = when (this) {
        is VedtakHendelseMelding -> "VedtakHendelseMelding(sakstype=$sakstype, medlemskapsperioder=${medlemskapsperioder.size})"
        else -> this::class.simpleName ?: "HendelseMelding"
    }
}
