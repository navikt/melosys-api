package no.nav.melosys.service.avgift

import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import org.springframework.stereotype.Service

@Service
class TrygdeavgiftMottakerService {

    fun skalBetalesTilNav(trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag): Boolean {
        val trygdeavgiftMottaker = getTrygdeavgiftMottaker(trygdeavgiftsgrunnlag)
        return trygdeavgiftMottaker == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
            || trygdeavgiftMottaker == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT
    }

    fun getTrygdeavgiftMottaker(trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag) = when {
        betalerKunTrygdeavgiftTilSkatt(trygdeavgiftsgrunnlag) -> Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT
        betalerKunTrygdeavgiftTilNav(trygdeavgiftsgrunnlag) -> Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
        else -> Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT
    }

    fun betalerKunTrygdeavgiftTilSkatt(trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag): Boolean {
        return trygdeavgiftsgrunnlag.skatteforholdTilNorge.all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }
            && trygdeavgiftsgrunnlag.inntektsperioder.all { it.isArbeidsgiversavgiftBetalesTilSkatt || erMisjonær(it.type) }
    }

    private fun betalerKunTrygdeavgiftTilNav(trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag): Boolean {
        return (trygdeavgiftsgrunnlag.skatteforholdTilNorge.all { it.skatteplikttype == Skatteplikttype.IKKE_SKATTEPLIKTIG }
            && trygdeavgiftsgrunnlag.inntektsperioder.all { !it.isArbeidsgiversavgiftBetalesTilSkatt || erMisjonær(it.type) })
            || trygdeavgiftsgrunnlag.inntektsperioder.all { erFnAnsatt(it.type) }
    }

    private fun erFnAnsatt(inntektskildetype: Inntektskildetype): Boolean {
        return inntektskildetype == Inntektskildetype.FN_SKATTEFRITAK
    }

    private fun erMisjonær(inntektskildetype: Inntektskildetype): Boolean {
        return inntektskildetype == Inntektskildetype.MISJONÆR
    }
}
