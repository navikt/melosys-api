package no.nav.melosys.service.avgift.satsendring

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*
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
    fun finnBehandlingerMedSatsendringer(år: Int): AvgiftSatsendringInfo {
        // Finn alle resultater med vedtak + trygdeavgift i oppgitt år
        val behandlingsresultatList = behandlingsresultatService.finnResultaterMedMedlemskapseriodeOverlappendeMed(år)
            .filter { trygdeavgiftService.harFakturerbarTrygdeavgift(it) }

        val behandlingList = behandlingsresultatList.map { behandlingService.hentBehandling(it.id) to harSatsendring(it) }.map {
            Behandling(
                it.first.id, it.first.fagsak.saksnummer, it.first.type, it.second
            )
        }

        return AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = behandlingList.filter { it.harSatsendring && it.behandlingstype != NY_VURDERING },
            behandlingerMedSatsendringOgNyVurdering = behandlingList.filter { it.harSatsendring && it.behandlingstype == NY_VURDERING },
            behandlingerUtenSatsendring = behandlingList.filterNot { it.harSatsendring }
        )
    }

    private fun harSatsendring(behandlingsresultat: Behandlingsresultat): Boolean {
        return true // TODO: Implementer
    }

    data class Behandling(
        val behandlingID: Long,
        val saksnummer: String,
        val behandlingstype: Behandlingstyper,
        val harSatsendring: Boolean
    )

    data class AvgiftSatsendringInfo(
        val år: Int,
        val behandlingerMedSatsendring: List<Behandling>,
        val behandlingerMedSatsendringOgNyVurdering: List<Behandling>,
        val behandlingerUtenSatsendring: List<Behandling>
    )
}
