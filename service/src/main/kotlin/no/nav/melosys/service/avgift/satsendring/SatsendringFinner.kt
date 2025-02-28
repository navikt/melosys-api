package no.nav.melosys.service.avgift.satsendring

import mu.KotlinLogging
import no.nav.melosys.domain.Behandling
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.*
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import no.nav.melosys.service.sak.FagsakService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

private val log = KotlinLogging.logger { }

@Component
class SatsendringFinner(
    private val behandlingService: BehandlingService,
    private val behandlingsresultatService: BehandlingsresultatService,
    private val trygdeavgiftService: TrygdeavgiftService,
    private val trygdeavgiftsberegningService: TrygdeavgiftsberegningService,
) {
    @Transactional(readOnly = true, noRollbackFor = [Throwable::class])
    fun finnBehandlingerMedSatsendring(år: Int): AvgiftSatsendringInfo {
        log.info { "Søker satsendringer for år: $år" }

        val behandlingerMedOverlappendeÅrOgFakturerbarTrygdeavgift =
            behandlingsresultatService.finnResultaterMedMedlemskapseriodeOverlappendeMed(år)
                .filter { it.type in listOf(MEDLEM_I_FOLKETRYGDEN, FASTSATT_TRYGDEAVGIFT, FASTSATT_LOVVALGSLAND, FORELOEPIG_FASTSATT_LOVVALGSLAND) }
                .filter { trygdeavgiftService.harFakturerbarTrygdeavgift(it) }
                .map { behandlingService.hentBehandling(it.id) }

        val sisteAvsluttetBehandlingPåFagsakTilknyttetSatsendring =
            behandlingerMedOverlappendeÅrOgFakturerbarTrygdeavgift
                .groupBy { it.fagsak }
                .filterNot { it.key.status in FagsakService.UGYLDIGE_SAKSSTATUSER_FOR_TRYGDEAVGIFT }
                .mapNotNull { (_, behandlinger) ->
                    behandlinger
                        .filter { it.type in listOf(FØRSTEGANG, NY_VURDERING, SATSENDRING, ENDRET_PERIODE) }
                        .filter { it.erAvsluttet() }
                        .maxByOrNull { it.registrertDato }
                }

        val behandlingerMedOverlappOgTrygdeavgiftSomErSistRegistrert =
            behandlingerMedOverlappendeÅrOgFakturerbarTrygdeavgift.intersect(sisteAvsluttetBehandlingPåFagsakTilknyttetSatsendring)

        log.debug { "Fant ${behandlingerMedOverlappOgTrygdeavgiftSomErSistRegistrert.size} behandlinger for år: $år" }

        val behandlingerForSatsendring = behandlingerMedOverlappOgTrygdeavgiftSomErSistRegistrert.map {
            try {
                BehandlingForSatstendring(
                    behandlingID = it.id,
                    saksnummer = it.fagsak.saksnummer,
                    behandlingstype = it.type,
                    harSatsendring = harSatsendring(it, år),
                    harAktivNyVurdering = harAktivNyVurdering(it)
                )
            } catch (t: Throwable) {
                log.warn { "SatsendringFinner feiler for behandlingID: ${it.id}: $t" }
                BehandlingForSatstendring(
                    behandlingID = it.id,
                    saksnummer = it.fagsak.saksnummer,
                    behandlingstype = it.type,
                    harSatsendring = false,
                    harAktivNyVurdering = false,
                    feilAarsak = t.message
                )
            }
        }

        val (behandlingForSatstendringerOk, behandlingForSatstendringerFeilet) = behandlingerForSatsendring.partition { it.feilAarsak == null }
        val (behandlingerMedSatsendringer, behandlingerUtenSatsendringer) = behandlingForSatstendringerOk.partition { it.harSatsendring }
        val (behandlingerMedSatsendringOgNyVurdering, behandlingerMedKunSatsendringer) = behandlingerMedSatsendringer.partition { it.harAktivNyVurdering }

        val avgiftSatsendringInfo = AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = behandlingerMedKunSatsendringer,
            behandlingerMedSatsendringOgNyVurdering = behandlingerMedSatsendringOgNyVurdering,
            behandlingerUtenSatsendring = behandlingerUtenSatsendringer,
            behandlingerSomFeilet = behandlingForSatstendringerFeilet
        )

        log.info { "Fant ${avgiftSatsendringInfo.behandlingerMedSatsendring.size} behandlinger med satsendring, uten ny vurdering" }
        log.info { "Fant ${avgiftSatsendringInfo.behandlingerMedSatsendringOgNyVurdering.size} behandlinger med satsendring og aktiv ny vurdering" }
        if (avgiftSatsendringInfo.behandlingerSomFeilet.isNotEmpty()) {
            log.warn { "${avgiftSatsendringInfo.behandlingerSomFeilet.size} behandlinger feiler når ev. satsendring sjekkes" }
        }

        return avgiftSatsendringInfo
    }

    private fun harSatsendring(behandling: Behandling, år: Int): Boolean {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)
        val nyTrygdeavgiftForÅr = trygdeavgiftsberegningService.beregnTrygdeavgift(
            behandlingsresultat,
            behandlingsresultat.hentSkatteforholdTilNorge().toList(),
            behandlingsresultat.hentInntektsperioder().toList(),
        ).filter { it.overlapperMedÅr(år) }

        val eksisterendeTrygdeavgiftsperioderForÅr = behandlingsresultat.trygdeavgiftsperioder
            .filter { it.overlapperMedÅr(år) }

        val erSatsEndret = nyTrygdeavgiftForÅr != eksisterendeTrygdeavgiftsperioderForÅr

        if (erSatsEndret) {
            log.info { "Satsendring i behandling ${behandlingsresultat.id}" }
            log.info { "Nye trygdeavgiftsperioder beregnet: $nyTrygdeavgiftForÅr" }
            log.info { "Eksisterende trygdeavgiftsperioder: ${eksisterendeTrygdeavgiftsperioderForÅr}" }
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
