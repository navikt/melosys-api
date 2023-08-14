package no.nav.melosys.service.ftrl

import mu.KotlinLogging
import no.nav.melosys.domain.ftrl.ManglendeFakturabetalingMelding
import no.nav.melosys.service.sak.OpprettBehandlingForSak
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class ManglendeFakturabetalingConsumer(
    @Autowired private val opprettBehandlingForSak: OpprettBehandlingForSak
) {
    private val log = KotlinLogging.logger { }

    @KafkaListener(
        clientIdPrefix = "aiven-melosys-manglende-fakturabetaling-consumer",
        topics = ["\${kafka.aiven.manglende-fakturabetaling.topic}"],
        containerFactory = "aivenManglendeFakturabetalingMeldingListenerContainerFactory"
    )
    fun lesFakturaMottattMelding(
        consumerRecord: ConsumerRecord<String, ManglendeFakturabetalingMelding>
    ) {
        val fakturaMottattMelding = consumerRecord.value()
        try {
            opprettBehandlingForSak.opprettBehandlingManglendeInnbetaling(
                fakturaMottattMelding.behandlingId,
                fakturaMottattMelding.mottaksDato
            )
        } catch (e: Exception) {
            log.error("Feil ved mottak av FakturaMottatt med consumer record key ${consumerRecord.key()}", e)
        }
    }
}
