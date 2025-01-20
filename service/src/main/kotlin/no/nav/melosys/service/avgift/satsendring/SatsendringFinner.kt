package no.nav.melosys.service.avgift.satsendring

import no.nav.melosys.domain.Behandlingsresultat
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

        val behandlingerForSatsendring = behandlingsresultatList.map {
            val behandling = behandlingService.hentBehandling(it.id)
            BehandlingForSatstendring(
                behandling.id, behandling.fagsak.saksnummer, behandling.type, harSatsendring(it)
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
        val erEndret = beregnNyeTrygdeavgiftsperioder(behandlingsresultat).toSet() != behandlingsresultat.trygdeavgiftsperioder

        if (erEndret) {
            println("Forskjell i trygdeavgiftsperioder")
            println("Gammel: ${behandlingsresultat.trygdeavgiftsperioder}")
            println("Ny: ${beregnNyeTrygdeavgiftsperioder(behandlingsresultat)}")
        }
        return erEndret
    }

    private fun beregnNyeTrygdeavgiftsperioder(behandlingsresultat: Behandlingsresultat) = trygdeavgiftsberegningService.beregnTrygdeavgift(
        behandlingsresultat,
        behandlingsresultat.hentSkatteforholdTilNorge().toList(),
        behandlingsresultat.hentInntektsperioder().toList(),
    )

    data class AvgiftSatsendringInfo(
        val år: Int,
        val behandlingerMedSatsendring: List<BehandlingForSatstendring>,
        val behandlingerMedSatsendringOgNyVurdering: List<BehandlingForSatstendring>,
        val behandlingerUtenSatsendring: List<BehandlingForSatstendring>
    )

    data class BehandlingForSatstendring(
        val behandlingID: Long,
        val saksnummer: String,
        val behandlingstype: Behandlingstyper,
        val harSatsendring: Boolean
    )
}
