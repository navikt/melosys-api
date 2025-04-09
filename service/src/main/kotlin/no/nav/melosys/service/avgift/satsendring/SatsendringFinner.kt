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
            behandlingsresultatService.finnResultaterMedVedtakOgMedlemskapsperiodeOverlappendeMed(år)
                .filter { it.type in listOf(MEDLEM_I_FOLKETRYGDEN, FASTSATT_TRYGDEAVGIFT, FASTSATT_LOVVALGSLAND, FORELOEPIG_FASTSATT_LOVVALGSLAND) }
                .filter { trygdeavgiftService.harFakturerbarTrygdeavgift(it) }
                .map { behandlingService.hentBehandling(it.id) }

        val sisteAvsluttetBehandlingPåFagsakTilknyttetSatsendring =
            behandlingerMedOverlappendeÅrOgFakturerbarTrygdeavgift
                .asSequence()
                .map { it.fagsak }
                .filterNot { it.status in FagsakService.UGYLDIGE_SAKSSTATUSER_FOR_TRYGDEAVGIFT }
                .distinct()
                .mapNotNull { fagsak ->
                    fagsak.behandlinger
                        .filter { it.type in listOf(FØRSTEGANG, NY_VURDERING, SATSENDRING, ENDRET_PERIODE) }
                        .filter { it.erAvsluttet() }
                        .maxByOrNull { it.registrertDato }
                }.toSet()

        val behandlingerMedOverlappOgTrygdeavgiftSomErSistRegistrert =
            behandlingerMedOverlappendeÅrOgFakturerbarTrygdeavgift.intersect(sisteAvsluttetBehandlingPåFagsakTilknyttetSatsendring)

        log.debug { "Fant ${behandlingerMedOverlappOgTrygdeavgiftSomErSistRegistrert.size} behandlinger for år: $år" }

        val behandlingerForSatsendring = behandlingerMedOverlappOgTrygdeavgiftSomErSistRegistrert.map {
            try {
                BehandlingForSatstendring(
                    behandlingID = it.id,
                    saksnummer = it.fagsak.saksnummer,
                    behandlingstype = it.type,
                    påvirketAvSatsendring = harSatsendring(it, år),
                    harAnnenAktivBehandling = harFølgendePåvirketAktivBehandling(it)
                )
            } catch (t: Throwable) {
                log.warn { "SatsendringFinner feiler for behandlingID: ${it.id}: $t" }
                BehandlingForSatstendring(
                    behandlingID = it.id,
                    saksnummer = it.fagsak.saksnummer,
                    behandlingstype = it.type,
                    påvirketAvSatsendring = false,
                    harAnnenAktivBehandling = false,
                    feilÅrsak = t.message
                )
            }
        }

        val (behandlingerForSatstendringerOk, behandlingerMedFeil) = behandlingerForSatsendring.partition { it.feilÅrsak == null }
        val (behandlingerMedSatsendringer, behandlingerUtenSatsendringer) = behandlingerForSatstendringerOk.partition { it.påvirketAvSatsendring }
        val (behandlingerMedSatsendringOgNyVurdering, behandlingerMedKunSatsendringer) = behandlingerMedSatsendringer.partition { it.harAnnenAktivBehandling }

        val avgiftSatsendringInfo = AvgiftSatsendringInfo(
            år = år,
            behandlingerMedSatsendring = behandlingerMedKunSatsendringer,
            behandlingerMedSatsendringOgNyVurdering = behandlingerMedSatsendringOgNyVurdering,
            behandlingerUtenSatsendring = behandlingerUtenSatsendringer,
            behandlingerSomFeilet = behandlingerMedFeil
        )

        if (behandlingerMedSatsendringer.isNotEmpty() || behandlingerMedSatsendringOgNyVurdering.isNotEmpty()) {
            log.info { "Det finnes minst én behandling påvirket av satsendring for $år." }
            log.info { "Fant ${avgiftSatsendringInfo.behandlingerMedSatsendring.size} behandlinger påvirket av én satsendring i samme sak." }
            log.info { "Fant ${avgiftSatsendringInfo.behandlingerMedSatsendringOgNyVurdering.size} behandlinger påvirket av satsendring, med aktiv påfølgende behandling i samme sak." }
        } else {
            log.info { "Ingen behandlinger påvirkes av satsendringer for $år. Totalt finnes det ${avgiftSatsendringInfo.behandlingerUtenSatsendring.size} behandlinger." }
        }

        if (avgiftSatsendringInfo.behandlingerSomFeilet.isNotEmpty()) {
            log.warn { "${avgiftSatsendringInfo.behandlingerSomFeilet.size} behandlinger som trigger feil når det sjekkes for satsendringer." }
        }

        return avgiftSatsendringInfo
    }

    private fun harSatsendring(behandling: Behandling, år: Int): Boolean {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandling.id)
        val nyTrygdeavgiftForÅr = trygdeavgiftsberegningService.beregnTrygdeavgift(
            behandlingsresultat,
            behandlingsresultat.hentSkatteforholdTilNorge().toList(),
            behandlingsresultat.hentInntektsperioder().toList(),
        ).filter { it.overlapperMedÅr(år) }.toSet()

        val eksisterendeTrygdeavgiftsperioderForÅr = behandlingsresultat.trygdeavgiftsperioder
            .filter { it.overlapperMedÅr(år) }.toSet()

        val erSatsEndret = nyTrygdeavgiftForÅr != eksisterendeTrygdeavgiftsperioderForÅr

        if (erSatsEndret) {
            log.info { "Behandling ${behandlingsresultat.id} er påvirket av en satsendring" }
            log.info { "Nye trygdeavgiftsperioder beregnet: $nyTrygdeavgiftForÅr" }
            log.info { "Eksisterende trygdeavgiftsperioder: $eksisterendeTrygdeavgiftsperioderForÅr" }
        }

        return erSatsEndret
    }

    private fun harFølgendePåvirketAktivBehandling(behandling: Behandling) =
        behandling.fagsak.finnAktivBehandlingIkkeÅrsavregning()?.type in aktiveBehandlingstyperSomKanPåvirkes

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
        val påvirketAvSatsendring: Boolean,
        val harAnnenAktivBehandling: Boolean,
        val feilÅrsak: String? = null
    )

    companion object {
        // Behandlingstyper som kan påvirkes av satsendringer når de er aktive i en sak.
        val aktiveBehandlingstyperSomKanPåvirkes = setOf(NY_VURDERING, MANGLENDE_INNBETALING_TRYGDEAVGIFT)
    }
}
