package no.nav.melosys.service.ftrl

import mu.KotlinLogging
import no.nav.melosys.domain.ftrl.FakturaMottattMelding
import no.nav.melosys.service.sak.OpprettBehandlingForSak
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class FakturaMottattConsumer(
    @Autowired private val opprettBehandlingForSak: OpprettBehandlingForSak
) {
    private val log = KotlinLogging.logger { }

    @KafkaListener(
        clientIdPrefix = "aiven-melosys-fakturamottatt-consumer",
        topics = ["\${kafka.aiven.fakturamottatt.topic}"],
        containerFactory = "aivenFakturaMottattMeldingListenerContainerFactory"
    )
    fun lesFakturaMottattMelding(
        consumerRecord: ConsumerRecord<String, FakturaMottattMelding>
    ) {
        try {
            opprettBehandlingForSak.opprettBehandlingManglendeInnbetaling(consumerRecord.value().behandlingId)
        } catch (e: Exception) {
            log.error("Feil ved mottak av FakturaMottatt med consumer record key ${consumerRecord.key()}", e)
        }
    }
}
