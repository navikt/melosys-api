package no.nav.melosys.service.avgift

import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import org.springframework.stereotype.Service

@Service
class TrygdeavgiftsMottakerService {

    fun skalBetalesTilNav(trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag): Boolean {
        val trygdeavgiftMottaker = getTrygdeavgiftMottaker(trygdeavgiftsgrunnlag)
        return (trygdeavgiftMottaker == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
            || trygdeavgiftMottaker == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
    }

    fun getTrygdeavgiftMottaker(trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag): Trygdeavgiftmottaker {
        return if (betalerKunTrygdeavgiftTilSkatt(trygdeavgiftsgrunnlag)) Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT
        else return if (betalerKunTrygdeavgiftTilNav(trygdeavgiftsgrunnlag)) Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
        else return Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT
    }

    private fun betalerKunTrygdeavgiftTilSkatt(trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag): Boolean {
        return ((trygdeavgiftsgrunnlag.skatteforholdTilNorge.stream().allMatch {
            skatteforholdTilNorge: SkatteforholdTilNorge -> skatteforholdTilNorge.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }
            && trygdeavgiftsgrunnlag.inntektsperioder.stream().allMatch{
                inntektsperiode: Inntektsperiode -> inntektsperiode.isArbeidsgiversavgiftBetalesTilSkatt || erMisjonær(inntektsperiode.type)}))
    }

    private fun betalerKunTrygdeavgiftTilNav(trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag): Boolean {
        return trygdeavgiftsgrunnlag.skatteforholdTilNorge.stream().allMatch { skatteforholdTilNorge: SkatteforholdTilNorge ->
                skatteforholdTilNorge.skatteplikttype == Skatteplikttype.IKKE_SKATTEPLIKTIG }
            && trygdeavgiftsgrunnlag.inntektsperioder.stream().allMatch { inntektsperiode: Inntektsperiode ->
                !inntektsperiode.isArbeidsgiversavgiftBetalesTilSkatt || erMisjonær(inntektsperiode.type) }
            || trygdeavgiftsgrunnlag.inntektsperioder.stream().allMatch { inntektsperiode: Inntektsperiode -> erFnAnsatt(inntektsperiode.type) }
    }

    private fun erFnAnsatt(inntektskildetype: Inntektskildetype): Boolean {
        return inntektskildetype == Inntektskildetype.FN_SKATTEFRITAK
    }

    private fun erMisjonær(inntektskildetype: Inntektskildetype): Boolean {
        return inntektskildetype == Inntektskildetype.MISJONÆR
    }
}
