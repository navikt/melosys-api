package no.nav.melosys.saksflyt.steg.arsavregning

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Fagsak
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.saksflyt.steg.StegBehandler
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessSteg
import no.nav.melosys.saksflytapi.domain.Prosessinstans
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.avgift.aarsavregning.ÅrsavregningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import org.springframework.stereotype.Component
import java.time.LocalDate

private val log = KotlinLogging.logger { }

@Component
class OpprettÅrsavregningBehandling(
    private val fagsakService: FagsakService,
    private val trygdeavgiftService: TrygdeavgiftService,
    private val behandlingService: BehandlingService,
    private val behandslingsresultatService: BehandlingsresultatService,
    private val årsavregningService: ÅrsavregningService
) : StegBehandler {
    override fun inngangsSteg(): ProsessSteg {
        return ProsessSteg.OPPRETT_AARSAVREGNING_BEHANDLING
    }

    override fun utfør(prosessinstans: Prosessinstans) {
        val gjelderÅr = prosessinstans.getData(ProsessDataKey.GJELDER_ÅR).toInt()
        val sakMedTrygdeavgift = fagsakService.hentFagsak(prosessinstans.getData(ProsessDataKey.SAKSNUMMER))

        finnAktivÅrsavregningBehandling(sakMedTrygdeavgift, gjelderÅr)?.run {
            if (this.status != Behandlingsstatus.OPPRETTET) {
                status = Behandlingsstatus.VURDER_DOKUMENT
                behandlingService.lagre(this)
            }
            return
        }

        val trygdeavgiftsBehandlingMedRelevantPeriode =
            trygdeavgiftService.finnSistFakturerbarTrygdeavgiftsbehandlingForÅr(sakMedTrygdeavgift.saksnummer, gjelderÅr).also {
                if (it == null) log.info(
                    "Fant ingen behandlinger med overlappende lovvalgsperioder eller medlemskapsperioder for sak: ${
                        sakMedTrygdeavgift.saksnummer
                    } og år: $gjelderÅr"
                )
            } ?: return

        behandlingService.nyBehandling(
            sakMedTrygdeavgift,
            Behandlingsstatus.VURDER_DOKUMENT,
            Behandlingstyper.ÅRSAVREGNING,
            trygdeavgiftsBehandlingMedRelevantPeriode.tema,
            null,
            null,
            LocalDate.now(),
            Behandlingsaarsaktyper.MELDING_FRA_SKATT,
            null
        ).also { nyBehandling ->
            årsavregningService.opprettÅrsavregning(nyBehandling.id, gjelderÅr)
            prosessinstans.behandling = nyBehandling
        }
    }

    private fun finnAktivÅrsavregningBehandling(sakMedTrygdeavgift: Fagsak, gjelderÅr: Int): Behandling? {
        val årsAvregninger = sakMedTrygdeavgift.hentAktiveÅrsavregninger().also { if (it.isEmpty()) log.info("Fant ingen aktive årsavregninger") }
            .filter { behandslingsresultatService.hentBehandlingsresultat(it.id).årsavregning.aar == gjelderÅr }

        when {
            årsAvregninger.isEmpty() -> {
                log.info("Fant ingen aktive årsavregninger for år $gjelderÅr")
                return null
            }

            årsAvregninger.size > 1 -> {
                throw TekniskException("Flere aktive årsavregninger funnet")
            }

            else -> {
                log.info("Fant aktiv årsavregning for år $gjelderÅr")
                return årsAvregninger.single()
            }
        }
    }
}
