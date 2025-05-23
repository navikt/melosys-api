package no.nav.melosys.service.avgift

import mu.KotlinLogging
import no.nav.melosys.domain.manglendebetaling.ManglendeFakturabetalingMelding
import no.nav.melosys.saksflytapi.ProsessinstansService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
@Profile("!local-q1 & !local-q2")
class ManglendeFakturabetalingConsumer(
    @Autowired private val prosessinstansService: ProsessinstansService
) {
    private val log = KotlinLogging.logger { }

    @KafkaListener(
        clientIdPrefix = "aiven-melosys-manglende-fakturabetaling-consumer",
        topics = ["\${kafka.aiven.manglende-fakturabetaling.topic}"],
        containerFactory = "aivenManglendeFakturabetalingMeldingListenerContainerFactory"
    )
    fun lesManglendeFakturabetalingMelding(
        consumerRecord: ConsumerRecord<String, ManglendeFakturabetalingMelding>
    ) {
        val manglendeFakturebetalingMelding = consumerRecord.value()
        try {
            prosessinstansService.opprettManglendeInnbetalingProsess(manglendeFakturebetalingMelding)
        } catch (e: Exception) {
            log.error("Feil ved mottak av ManglendeFakturabetaling med fakturanummer ${consumerRecord.value().fakturanummer}")
            throw e
        }
    }
}
