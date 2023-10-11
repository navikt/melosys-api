package no.nav.melosys.service.avgift

import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsgrunnlag
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import org.springframework.stereotype.Service
import java.util.function.Predicate

@Service
class TrygdeavgiftsMottakerService {

    fun skalBetalesTilNav(trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag): Boolean {
        val trygdeavgiftMottaker = getTrygdeavgiftMottaker(trygdeavgiftsgrunnlag)
        return (trygdeavgiftMottaker == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
            || trygdeavgiftMottaker == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT)
    }

    fun getTrygdeavgiftMottaker(trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag): Trygdeavgiftmottaker {
        if (trygdeavgiftBetalesTilNavOgSkatt(trygdeavgiftsgrunnlag)) {
            return Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT
        }
        return if (trygdeavgiftBetalesTilSkatt(trygdeavgiftsgrunnlag)) Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT else Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
    }

    private fun trygdeavgiftBetalesTilNavOgSkatt(trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag): Boolean {
        return trygdeavgiftBetalesTilSkatt(trygdeavgiftsgrunnlag) == trygdeavgiftBetalesTilNav(trygdeavgiftsgrunnlag)
    }

    private fun trygdeavgiftBetalesTilNav(trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag): Boolean {
        return betalerKunArbeidsGiveravgiftOgTrygdeavgiftTilNav(trygdeavgiftsgrunnlag) || erSpesielGruppeOgIkkeSkattepliktig(trygdeavgiftsgrunnlag)
    }

    private fun trygdeavgiftBetalesTilSkatt(trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag): Boolean {
        return betalerKunArbeidsGiveravgiftOgTrygdeavgiftTilSkatt(trygdeavgiftsgrunnlag) || erSpesielGruppeOgSkattepliktig(trygdeavgiftsgrunnlag)
    }

    private fun betalerKunArbeidsGiveravgiftOgTrygdeavgiftTilSkatt(trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag): Boolean {
        return (trygdeavgiftsgrunnlag.getSkatteforholdTilNorge().stream().allMatch(
            Predicate<SkatteforholdTilNorge> { skatteforholdTilNorge: SkatteforholdTilNorge -> skatteforholdTilNorge.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG })
            && trygdeavgiftsgrunnlag.getInntektsperioder().stream()
            .allMatch(Predicate<Inntektsperiode> { obj: Inntektsperiode -> obj.isArbeidsgiversavgiftBetalesTilSkatt }))
    }

    private fun betalerKunArbeidsGiveravgiftOgTrygdeavgiftTilNav(trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag): Boolean {
        return (trygdeavgiftsgrunnlag.getSkatteforholdTilNorge().stream().allMatch(
            Predicate<SkatteforholdTilNorge> { skatteforholdTilNorge: SkatteforholdTilNorge -> skatteforholdTilNorge.skatteplikttype == Skatteplikttype.IKKE_SKATTEPLIKTIG })
            && trygdeavgiftsgrunnlag.getInntektsperioder().stream()
            .noneMatch(Predicate<Inntektsperiode> { obj: Inntektsperiode -> obj.isArbeidsgiversavgiftBetalesTilSkatt }))
    }

    private fun erSpesielGruppeOgSkattepliktig(trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag): Boolean {
        return trygdeavgiftsgrunnlag.getSkatteforholdTilNorge().stream()
            .anyMatch(Predicate<SkatteforholdTilNorge> { skatteforholdTilNorge: SkatteforholdTilNorge -> skatteforholdTilNorge.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }) && erMisjonær(trygdeavgiftsgrunnlag)
    }

    private fun erSpesielGruppeOgIkkeSkattepliktig(trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag): Boolean {
        return (trygdeavgiftsgrunnlag.getSkatteforholdTilNorge().stream()
            .anyMatch(Predicate<SkatteforholdTilNorge> { skatteforholdTilNorge: SkatteforholdTilNorge -> skatteforholdTilNorge.skatteplikttype == Skatteplikttype.IKKE_SKATTEPLIKTIG })
            && erMisjonær(trygdeavgiftsgrunnlag))
    }

    private fun erMisjonær(trygdeavgiftsgrunnlag: Trygdeavgiftsgrunnlag): Boolean {
        return trygdeavgiftsgrunnlag.getInntektsperioder().stream()
            .anyMatch(Predicate<Inntektsperiode> { inntektsperiode: Inntektsperiode -> inntektsperiode.type == Inntektskildetype.MISJONÆR && !inntektsperiode.isArbeidsgiversavgiftBetalesTilSkatt })
    }

}
