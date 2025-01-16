package no.nav.melosys.service.avgift.satsendring

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftsberegningResponse
import no.nav.melosys.service.avgift.TrygdeavgiftService
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService
import no.nav.melosys.service.avgift.idToUUid
import no.nav.melosys.service.behandling.BehandlingService
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

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
        val skatteforholdsperioderMedUUID = behandlingsresultat.hentSkatteforholdTilNorge().map { UUID.randomUUID() to it }
        val inntektsperioderMedUUID = behandlingsresultat.hentInntektsperioder().map { UUID.randomUUID() to it }

        val beregnetTrygdeavgiftList = trygdeavgiftsberegningService.beregnTrygdeavgift(
            behandlingsresultat,
            skatteforholdsperioderMedUUID,
            inntektsperioderMedUUID
        )

        val nyeTrygdeavgiftsperioder = beregnetTrygdeavgiftList.map { beregnetAvgiftPerPeriode ->
            lagTrygdeavgiftsperiode(beregnetAvgiftPerPeriode, skatteforholdsperioderMedUUID, inntektsperioderMedUUID, behandlingsresultat)
        }

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

    // TODO: bør nok ikke være her
    private fun lagTrygdeavgiftsperiode(
        response: TrygdeavgiftsberegningResponse,
        skatteforholdsperioderMedUUID: List<Pair<UUID, SkatteforholdTilNorge>>,
        inntektsperioderMedUUID: List<Pair<UUID, Inntektsperiode>>,
        behandlingsresultat: Behandlingsresultat
    ): Trygdeavgiftsperiode {
        val medlemskapsperiodeID = response.grunnlag.medlemskapsperiodeId
        val grunnlagMedlemskapsperiode = behandlingsresultat.medlemskapsperioder
            .firstOrNull { idToUUid(it.id) == medlemskapsperiodeID }
            ?: throw IllegalStateException("Fant ikke medlemskapsperiode $medlemskapsperiodeID")

        val skatteforholdsperiodeID =  response.grunnlag.skatteforholdsperiodeId
        val grunnlagSkatteforholdTilNorge = skatteforholdsperioderMedUUID
            .find { it.first == skatteforholdsperiodeID }?.second
            ?: throw IllegalStateException("Fant ikke skatteforholdsperiode $skatteforholdsperiodeID")

        val inntektsperiodeID = response.grunnlag.inntektsperiodeId
        val grunnlagInntekstperiode = inntektsperioderMedUUID
            .find { it.first == inntektsperiodeID }?.second
            ?: throw IllegalStateException("Fant ikke inntektsperiode $inntektsperiodeID")

        val trygdeavgiftsperiode = Trygdeavgiftsperiode(
            periodeFra = response.beregnetPeriode.periode.fom,
            periodeTil = response.beregnetPeriode.periode.tom,
            trygdesats = response.beregnetPeriode.sats,
            trygdeavgiftsbeløpMd = response.beregnetPeriode.månedsavgift.tilPenger(),
            grunnlagMedlemskapsperiode = grunnlagMedlemskapsperiode,
            grunnlagSkatteforholdTilNorge = grunnlagSkatteforholdTilNorge,
            grunnlagInntekstperiode = grunnlagInntekstperiode
        )

        // Ingen sideEffekt her!
        // .also { grunnlagMedlemskapsperiode.trygdeavgiftsperioder.add(it) }

        return trygdeavgiftsperiode
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
