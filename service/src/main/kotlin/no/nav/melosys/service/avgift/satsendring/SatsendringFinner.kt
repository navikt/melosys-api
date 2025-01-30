package no.nav.melosys.service.avgift.satsendring

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.NY_VURDERING
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

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
                behandling.id, behandling.fagsak.saksnummer, behandling.type, harSatsendring(it), harAktivNyVurdering(behandling)
            )
        }

        return AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = behandlingerForSatsendring.filter { it.harSatsendring && !it.harAktivNyVurdering },
            behandlingerMedSatsendringOgNyVurdering = behandlingerForSatsendring.filter { it.harSatsendring && it.harAktivNyVurdering },
            behandlingerUtenSatsendring = behandlingerForSatsendring.filterNot { it.harSatsendring }
        )
    }

    private fun harSatsendring(behandlingsresultat: Behandlingsresultat): Boolean {
        val nyeTrygdeavgiftsperioder = beregnNyeTrygdeavgiftsperioder(behandlingsresultat).toSet()

        val erEndret = nyeTrygdeavgiftsperioder != behandlingsresultat.trygdeavgiftsperioder

        if (erEndret) {
            log.debug { "Forskjell i trygdeavgiftsperioder. Eksisterende: ${behandlingsresultat.trygdeavgiftsperioder}" }
            log.debug { "Nye trygdeavgiftsperioder: $nyeTrygdeavgiftsperioder" }
        }
        return erEndret
    }

    private fun beregnNyeTrygdeavgiftsperioder(behandlingsresultat: Behandlingsresultat) = trygdeavgiftsberegningService.beregnTrygdeavgift(
        behandlingsresultat,
        behandlingsresultat.hentSkatteforholdTilNorge().toList(),
        behandlingsresultat.hentInntektsperioder().toList(),
    )

    private fun harAktivNyVurdering(behandling: Behandling): Boolean = behandling.fagsak.finnAktivBehandlingIkkeÅrsavregning()?.type == NY_VURDERING

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
        val harSatsendring: Boolean,
        val harAktivNyVurdering: Boolean
    )
}
