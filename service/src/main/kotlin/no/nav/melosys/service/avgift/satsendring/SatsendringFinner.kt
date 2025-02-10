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
    @Transactional(readOnly = true, rollbackFor = [], noRollbackFor = [Throwable::class])
    fun finnBehandlingerMedSatsendring(år: Int): AvgiftSatsendringInfo {
        val behandlingsresultatList = behandlingsresultatService.finnResultaterMedMedlemskapseriodeOverlappendeMed(år)
            .filter { trygdeavgiftService.harFakturerbarTrygdeavgift(it) }

        val behandlingerForSatsendring = behandlingsresultatList.map {
            val behandling = behandlingService.hentBehandling(it.id)

            try {
                val harEndring = harSatsendring(it)

                BehandlingForSatstendring(
                    behandlingID = behandling.id,
                    saksnummer = behandling.fagsak.saksnummer,
                    behandlingstype = behandling.type,
                    harSatsendring = harEndring,
                    harAktivNyVurdering = harAktivNyVurdering(behandling)
                )
            } catch (e: Exception) {
                log.error { "SatsendringFinner feiler med behandlingID: ${it.id}: ${e.message}" }
                BehandlingForSatstendring(
                    behandlingID = behandling.id,
                    saksnummer = behandling.fagsak.saksnummer,
                    behandlingstype = behandling.type,
                    harSatsendring = false,
                    harAktivNyVurdering = harAktivNyVurdering(behandling),
                    feilAarsak = e.message
                )
            }
        }

        val (behandlingForSatstendringerOk, behandlingForSatstendringerFeilet) = behandlingerForSatsendring.partition { it.feilAarsak == null }

        return AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = behandlingForSatstendringerOk.filter { it.harSatsendring && !it.harAktivNyVurdering },
            behandlingerMedSatsendringOgNyVurdering = behandlingForSatstendringerOk.filter { it.harSatsendring && it.harAktivNyVurdering },
            behandlingerUtenSatsendring = behandlingForSatstendringerOk.filterNot { it.harSatsendring },
            behandlingerSomFeilet = behandlingForSatstendringerFeilet
        )
    }

    private fun harSatsendring(behandlingsresultat: Behandlingsresultat): Boolean {
        val nyTrygdeavgift = trygdeavgiftsberegningService.beregnTrygdeavgift(
            behandlingsresultat,
            behandlingsresultat.hentSkatteforholdTilNorge().toList(),
            behandlingsresultat.hentInntektsperioder().toList(),
        )

        val nyeTrygdeavgiftsperioder = nyTrygdeavgift.toSet()
        val erSatsEndret = nyeTrygdeavgiftsperioder != behandlingsresultat.trygdeavgiftsperioder

        if (erSatsEndret) {
            log.debug { "Forskjell i trygdeavgiftsperioder. Eksisterende: ${behandlingsresultat.trygdeavgiftsperioder}" }
            log.debug { "Nye trygdeavgiftsperioder: $nyeTrygdeavgiftsperioder" }
        }

        return erSatsEndret
    }

    private fun harAktivNyVurdering(behandling: Behandling): Boolean = behandling.fagsak.finnAktivBehandlingIkkeÅrsavregning()?.type == NY_VURDERING

    data class AvgiftSatsendringInfo(
        val år: Int,
        val behandlingerMedSatsendring: List<BehandlingForSatstendring>,
        val behandlingerMedSatsendringOgNyVurdering: List<BehandlingForSatstendring>,
        val behandlingerUtenSatsendring: List<BehandlingForSatstendring>,
        val behandlingerSomFeilet: List<BehandlingForSatstendring>
    )

    data class BehandlingForSatstendring(
        val behandlingID: Long,
        val saksnummer: String,
        val behandlingstype: Behandlingstyper,
        val harSatsendring: Boolean,
        val harAktivNyVurdering: Boolean,
        val feilAarsak: String? = null
    )
}
