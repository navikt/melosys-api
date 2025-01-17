package no.nav.melosys.service.avgift.satsendring

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.NY_VURDERING
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class SatsendringFinner(
    private val behandlingService: BehandlingService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val trygdeavgiftService: TrygdeavgiftService,
    private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService
) {
    @Transactional(readOnly = true)
    fun finnBehandlingerMedSatsendring(år: Int): AvgiftSatsendringInfo {
        // Finn alle resultater med vedtak + trygdeavgift i oppgitt år
        val behandlingsresultatList = behandlingsresultatService.finnResultaterMedMedlemskapseriodeOverlappendeMed(år)
            .filter { trygdeavgiftService.harFakturerbarTrygdeavgift(it) }

        val behandlingerForSatsendring = behandlingsresultatList.map { behandlingService.hentBehandling(it.id) to harSatsendring(it) }.map {
            BehandlingForSatstendring(
                it.first.id, it.first.fagsak.saksnummer, it.first.type, it.second
            )
        }

        return AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = behandlingerForSatsendring.filter { it.harSatsendring && it.behandlingstype != NY_VURDERING },
            behandlingerMedSatsendringOgNyVurdering = behandlingerForSatsendring.filter { it.harSatsendring && it.behandlingstype == NY_VURDERING },
            behandlingerUtenSatsendring = behandlingerForSatsendring.filterNot { it.harSatsendring }
        )
    }

    private fun harSatsendring(behandlingsresultat: Behandlingsresultat): Boolean {
        val nyeTrygdeavgiftsperioder = trygdeavgiftsberegningService.beregnTrygdeavgift(
            behandlingsresultat,
            behandlingsresultat.hentSkatteforholdTilNorge().toList(),
            behandlingsresultat.hentInntektsperioder().toList(),
        )

        return sammenlignTrygdeavgiftsperioder(behandlingsresultat.trygdeavgiftsperioder, nyeTrygdeavgiftsperioder)
    }

    private fun sammenlignTrygdeavgiftsperioder(trygdeavgiftsperioder: Set<Trygdeavgiftsperiode>, nyeTrygdeavgiftsperioder: List<Trygdeavgiftsperiode>): Boolean {
        val erForskjellige = nyeTrygdeavgiftsperioder.size != trygdeavgiftsperioder.size || !trygdeavgiftsperioder.containsAll(nyeTrygdeavgiftsperioder)
        if (erForskjellige) {
            println("Forskjell i trygdeavgiftsperioder")
            println("Gammel: $trygdeavgiftsperioder")
            println("Ny: $nyeTrygdeavgiftsperioder")
        }
        return erForskjellige
    }

    data class BehandlingForSatstendring(
        val behandlingID: Long,
        val saksnummer: String,
        val behandlingstype: Behandlingstyper,
        val harSatsendring: Boolean
    )

    data class AvgiftSatsendringInfo(
        val år: Int,
        val behandlingerMedSatsendring: List<BehandlingForSatstendring>,
        val behandlingerMedSatsendringOgNyVurdering: List<BehandlingForSatstendring>,
        val behandlingerUtenSatsendring: List<BehandlingForSatstendring>
    )
}
