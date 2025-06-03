package no.nav.melosys.service.avgift

import jakarta.transaction.Transactional
import mu.KotlinLogging
import no.nav.melosys.domain.manglendebetaling.ManglendeFakturabetalingMelding
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
@Profile("!local-q1 & !local-q2")
class ManglendeFakturabetalingConsumer(
    @Autowired private val prosessinstansService: ProsessinstansService,
    @Autowired private val behandlingsresultatService: BehandlingsresultatService
) {
    private val log = KotlinLogging.logger { }

    @Transactional
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
            val fakturaserieReferanse = manglendeFakturebetalingMelding.fakturaserieReferanse
            val sisteResultatMedReferanse = behandlingsresultatService
                .finnAlleBehandlingsresultatMedFakturaserieReferanse(fakturaserieReferanse)
                .sortedByDescending { it.registrertDato }
                .ifEmpty {
                    throw FunksjonellException("Finner ikke behandlingsresultat med fakturaserie-referanse: $fakturaserieReferanse")
                }.first()

            if (sisteResultatMedReferanse.medlemskapsperioder.isNotEmpty() && sisteResultatMedReferanse.medlemskapsperioder.all { it.erPliktig() }) {
                prosessinstansService.opprettProsessManglendeInnbetalingVarselBrev(
                    sisteResultatMedReferanse.behandling,
                    manglendeFakturebetalingMelding
                )
            } else {
                prosessinstansService.opprettProsessManglendeInnbetalingBehandling(manglendeFakturebetalingMelding)
            }
        } catch (e: Exception) {
            log.error("Feil ved mottak av ManglendeFakturabetaling med fakturanummer ${consumerRecord.value().fakturanummer}")
            throw e
        }
    }
}
