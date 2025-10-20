package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import no.nav.melosys.service.behandling.BehandlingsresultatService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TrygdeavgiftMottakerService(private val behandlingsresultatService: BehandlingsresultatService) {

    fun skalBetalesTilNav(behandlingsresultat: Behandlingsresultat): Boolean {
        val trygdeavgiftMottaker = getTrygdeavgiftMottaker(behandlingsresultat)
        return trygdeavgiftMottaker == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
            || trygdeavgiftMottaker == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT
    }

    @Transactional(readOnly = true)
    fun getTrygdeavgiftMottaker(behandlingID: Long): Trygdeavgiftmottaker {
        val behandlingsresultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID)
        var trygdeavgiftsperioder = behandlingsresultat.trygdeavgiftsperioder.toList()

        if(behandlingsresultat.hentBehandling().erEøsPensjonist()){
            trygdeavgiftsperioder = behandlingsresultat.eøsPensjonistTrygdeavgiftsperioder.toList()
        }

        return getTrygdeavgiftMottaker(trygdeavgiftsperioder)
    }

    fun getTrygdeavgiftMottaker(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>) =
        getTrygdeavgiftMottaker(
            trygdeavgiftsperioder.mapNotNull { it.grunnlagSkatteforholdTilNorge }.toSet(),
            trygdeavgiftsperioder.mapNotNull { it.grunnlagInntekstperiode }.toSet()
        )

    @Deprecated("Behøver kun trygdeavgiftsperioder")
    fun getTrygdeavgiftMottaker(behandlingsresultat: Behandlingsresultat) =
        getTrygdeavgiftMottaker(
            behandlingsresultat.hentSkatteforholdTilNorge(),
            behandlingsresultat.hentInntektsperioder()
        )

    private fun getTrygdeavgiftMottaker(skatteforhold: Set<SkatteforholdTilNorge>, inntektsperioder: Set<Inntektsperiode>) =
        when {
            betalerKunTrygdeavgiftTilSkatt(skatteforhold, inntektsperioder) -> Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT
            betalerKunTrygdeavgiftTilNav(skatteforhold, inntektsperioder) -> Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
            else -> Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT
        }

    private fun betalerKunTrygdeavgiftTilSkatt(skatteforholdTilNorge: Set<SkatteforholdTilNorge>, inntektsperioder: Set<Inntektsperiode>): Boolean =
        skatteforholdTilNorge.all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }
            && inntektsperioder.all { it.isArbeidsgiversavgiftBetalesTilSkatt || erMisjonær(it.type) }

    private fun betalerKunTrygdeavgiftTilNav(skatteforholdTilNorge: Set<SkatteforholdTilNorge>, inntektsperioder: Set<Inntektsperiode>): Boolean =
        (skatteforholdTilNorge.all { it.skatteplikttype == Skatteplikttype.IKKE_SKATTEPLIKTIG }
            && inntektsperioder.all { !it.isArbeidsgiversavgiftBetalesTilSkatt || erMisjonær(it.type) })
            || inntektsperioder.all { erFnAnsatt(it.type) }

    private fun erFnAnsatt(inntektskildetype: Inntektskildetype): Boolean = inntektskildetype == Inntektskildetype.FN_SKATTEFRITAK

    private fun erMisjonær(inntektskildetype: Inntektskildetype): Boolean = inntektskildetype == Inntektskildetype.MISJONÆR
}
