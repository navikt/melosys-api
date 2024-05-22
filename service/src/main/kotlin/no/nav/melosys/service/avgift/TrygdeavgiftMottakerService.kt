package no.nav.melosys.service.avgift

import no.nav.melosys.domain.folketrygden.FastsattTrygdeavgift
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import no.nav.melosys.service.avgift.dto.OppdaterTrygdeavgiftsgrunnlagRequest
import org.springframework.stereotype.Service

@Service
class TrygdeavgiftMottakerService {

    fun skalBetalesTilNav(
        fastsattTrygdeavgift: FastsattTrygdeavgift,
        oppdaterTrygdeavgiftsgrunnlagRequest: OppdaterTrygdeavgiftsgrunnlagRequest? = null
    ): Boolean {
        val trygdeavgiftMottaker = getTrygdeavgiftMottaker(fastsattTrygdeavgift, oppdaterTrygdeavgiftsgrunnlagRequest)
        return trygdeavgiftMottaker == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
            || trygdeavgiftMottaker == Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT
    }

    fun getTrygdeavgiftMottaker(
        fastsattTrygdeavgift: FastsattTrygdeavgift,
        oppdaterTrygdeavgiftsgrunnlagRequest: OppdaterTrygdeavgiftsgrunnlagRequest? = null
    ) = when {
        betalerKunTrygdeavgiftTilSkatt(
            fastsattTrygdeavgift,
            oppdaterTrygdeavgiftsgrunnlagRequest
        ) -> Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_SKATT

        betalerKunTrygdeavgiftTilNav(fastsattTrygdeavgift, oppdaterTrygdeavgiftsgrunnlagRequest) -> Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV
        else -> Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT
    }

    fun betalerKunTrygdeavgiftTilSkatt(
        fastsattTrygdeavgift: FastsattTrygdeavgift,
        oppdaterTrygdeavgiftsgrunnlagRequest: OppdaterTrygdeavgiftsgrunnlagRequest? = null
    ): Boolean {
        if (oppdaterTrygdeavgiftsgrunnlagRequest != null) {
            return oppdaterTrygdeavgiftsgrunnlagRequest.skatteforholdTilNorgeList.all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }
                && oppdaterTrygdeavgiftsgrunnlagRequest.inntektskilder.all { it.arbeidsgiversavgiftBetales ||  it.type == Inntektskildetype.MISJONÆR }
        }

        //TODO: er .filterNotNull() hack? - sjekk YrkesaktivFtrlVedtakIT uten dette.
        return fastsattTrygdeavgift.hentSkatteforholdTilNorge().filterNotNull().all { it.skatteplikttype == Skatteplikttype.SKATTEPLIKTIG }
            && fastsattTrygdeavgift.hentInntektsperioder().filterNotNull().all { it.isArbeidsgiversavgiftBetalesTilSkatt || erMisjonær(it.type) }
    }

    private fun betalerKunTrygdeavgiftTilNav(
        fastsattTrygdeavgift: FastsattTrygdeavgift,
        oppdaterTrygdeavgiftsgrunnlagRequest: OppdaterTrygdeavgiftsgrunnlagRequest? = null
    ): Boolean {
        if (oppdaterTrygdeavgiftsgrunnlagRequest != null) {
            return oppdaterTrygdeavgiftsgrunnlagRequest.skatteforholdTilNorgeList.all { it.skatteplikttype == Skatteplikttype.IKKE_SKATTEPLIKTIG }
                && oppdaterTrygdeavgiftsgrunnlagRequest.inntektskilder.all { !it.arbeidsgiversavgiftBetales || erMisjonær(it.type) }
                || oppdaterTrygdeavgiftsgrunnlagRequest.inntektskilder.all { erFnAnsatt(it.type) }
        }

        return (fastsattTrygdeavgift.hentSkatteforholdTilNorge().all { it.skatteplikttype == Skatteplikttype.IKKE_SKATTEPLIKTIG }
            && fastsattTrygdeavgift.hentInntektsperioder().all { !it.isArbeidsgiversavgiftBetalesTilSkatt || erMisjonær(it.type) })
            || fastsattTrygdeavgift.hentInntektsperioder().all { erFnAnsatt(it.type) }
    }

    private fun erFnAnsatt(inntektskildetype: Inntektskildetype): Boolean {
        return inntektskildetype == Inntektskildetype.FN_SKATTEFRITAK
    }

    private fun erMisjonær(inntektskildetype: Inntektskildetype): Boolean {
        return inntektskildetype == Inntektskildetype.MISJONÆR
    }
}
