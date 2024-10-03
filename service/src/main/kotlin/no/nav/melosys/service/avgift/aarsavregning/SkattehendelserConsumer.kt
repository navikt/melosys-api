package no.nav.melosys.service.avgift.aarsavregning

import io.getunleash.Unleash
import jakarta.transaction.Transactional
import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.avgift.aarsavregning.Skattehendelse
import no.nav.melosys.domain.kodeverk.Aktoersroller
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.saksflytapi.ProsessinstansService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger { }

@Profile("!local-q1 & !local-q2")
@Service
class SkattehendelserConsumer(
    @Autowired private val prosessinstansService: ProsessinstansService,
    @Autowired private val unleash: Unleash,
    @Autowired private val fagsakService: FagsakService,
    @Autowired private val behandlingService: BehandlingService,
    @Autowired private val behandslingsresultatService: BehandlingsresultatService,
    @Autowired private val årsavregningService: ÅrsavregningService
) {

    @KafkaListener(
        clientIdPrefix = "aiven-melosys-skattehendelser-consumer",
        topics = ["\${kafka.aiven.skattehendelser.topic}"],
        containerFactory = "aivenSkattehendelserListenerContainerFactory"
    )
    @Transactional
    fun lesSkattehendelser(consumerRecord: ConsumerRecord<String, Skattehendelse>) {
        if (unleash.isEnabled(ToggleName.MELOSYS_SKATTEHENDELSE_CONSUMER)) {
            val skattehendelse = consumerRecord.value()
            val sakerMedTrygdeavgift = finnSakMedTrygdeavgift(skattehendelse.identifikator, skattehendelse.gjelderPeriode.toInt())
            if (sakerMedTrygdeavgift.isEmpty()) {
                log.warn { "Fant ingen sak med trygdeavgift for aktør: $skattehendelse.identifikator" }
            }
            for (fagsak in sakerMedTrygdeavgift) {
                if (skalOpprettArsavregningsBehandlingProsessflyt(fagsak, skattehendelse.gjelderPeriode.toInt())) {
                    prosessinstansService.opprettArsavregningsBehandlingProsessflyt(fagsak.saksnummer, skattehendelse.gjelderPeriode)
                }
            }
        } else {
            log.info { "Skattehendelsemelding med key: ${consumerRecord.key()}" }
        }
    }


    private fun finnSakMedTrygdeavgift(aktørId: String, år: Int): List<Fagsak> {
        return fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, aktørId)
            .filter {
                årsavregningService.hentSisteBehandlingsresultatMedInnvilgetMedlemskapsperiodeOgAvgiftsgrunnlag(it.saksnummer, år) != null
            }
    }

    private fun skalOpprettArsavregningsBehandlingProsessflyt(sakMedTrygdeavgift: Fagsak, gjelderÅr: Int): Boolean {
        finnAktivÅrsavregningBehandling(sakMedTrygdeavgift, gjelderÅr)?.let { behandling ->
            log.info { "Årsavregning behandling(${behandling.id}) for sak: ${sakMedTrygdeavgift.saksnummer} og år: $gjelderÅr er allerede opprettet" }
            if (behandling.status != Behandlingsstatus.OPPRETTET) {
                log.info { "Oppdaterer status fra ${behandling.status} til VURDER_DOKUMENT for behandling ${behandling.id}" }
                behandling.status = Behandlingsstatus.VURDER_DOKUMENT
                behandlingService.lagre(behandling)
            }
            return false
        }

        // Dette kan vel slettes
        val trygdeavgiftsBehandlingtMedRelevantPeriode =
            årsavregningService.hentSisteBehandlingsresultatMedInnvilgetMedlemskapsperiodeOgAvgiftsgrunnlag(
                sakMedTrygdeavgift.saksnummer,
                gjelderÅr
            )?.behandling

        if (trygdeavgiftsBehandlingtMedRelevantPeriode == null) {
            log.info(
                "Fant ingen behandlinger med overlappende trygdeavgiftsperiode for sak: ${sakMedTrygdeavgift.saksnummer} og år: $gjelderÅr. Avslutter steg"
            )
            return false
        }

        return true
    }

    private fun finnAktivÅrsavregningBehandling(sakMedTrygdeavgift: Fagsak, gjelderÅr: Int): Behandling? {
        val årsAvregninger = sakMedTrygdeavgift.hentAktiveÅrsavregninger()
            .filter { behandslingsresultatService.hentBehandlingsresultat(it.id).årsavregning.aar == gjelderÅr }

        when {
            årsAvregninger.isEmpty() -> {
                log.info("Fant ingen aktive årsavregninger for år $gjelderÅr")
                return null
            }

            årsAvregninger.size > 1 -> {
                throw TekniskException("Flere aktive årsavregninger funnet for sak: ${sakMedTrygdeavgift.saksnummer} og år: $gjelderÅr")
            }

            else -> {
                log.info("Fant aktiv årsavregning for ${sakMedTrygdeavgift.saksnummer} og år $gjelderÅr")
                return årsAvregninger.single()
            }
        }
    }

}


