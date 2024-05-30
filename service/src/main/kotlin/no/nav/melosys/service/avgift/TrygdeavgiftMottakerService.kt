package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Behandlingsresultat
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import org.springframework.stereotype.Service

@Service
class TrygdeavgiftMottakerService {

    fun skalBetalesTilNav(
        behandlingsresultat: Behandlingsresultat,
    ): Boolean {
        val trygdeavgiftMottaker = getTrygdeavgiftMottaker(behandlingsresultat)
        return trygdeavgiftMottaker == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
            || trygdeavgiftMottaker == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT
    }

    fun getTrygdeavgiftMottaker(
        behandlingsresultat: Behandlingsresultat,
    ) = when {
        betalerKunTrygdeavgiftTilSkatt(
            behandlingsresultat,
        ) -> Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT

        betalerKunTrygdeavgiftTilNav(behandlingsresultat) -> Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
        else -> Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT
    }

    fun betalerKunTrygdeavgiftTilSkatt(
        behandlingsresultat: Behandlingsresultat,
    ): Boolean {
        return behandlingsresultat.hentSkatteforholdTilNorge().all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }
            && behandlingsresultat.hentInntektsperioder().filterNotNull().all { it.isArbeidsgiversavgiftBetalesTilSkatt || erMisjonær(it.type) }
    }

    fun betalerKunTrygdeavgiftTilNav(
        behandlingsresultat: Behandlingsresultat,
    ): Boolean {

        return (behandlingsresultat.hentSkatteforholdTilNorge().all { it.skatteplikttype == Skatteplikttype.IKKE_SKATTEPLIKTIG }
            && behandlingsresultat.hentInntektsperioder().all { !it.isArbeidsgiversavgiftBetalesTilSkatt || erMisjonær(it.type) })
            || behandlingsresultat.hentInntektsperioder().all { erFnAnsatt(it.type) }
    }

    private fun erFnAnsatt(inntektskildetype: Inntektskildetype): Boolean {
        return inntektskildetype == Inntektskildetype.FN_SKATTEFRITAK
    }

    private fun erMisjonær(inntektskildetype: Inntektskildetype): Boolean {
        return inntektskildetype == Inntektskildetype.MISJONÆR
    }
}
