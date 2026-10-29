package no.nav.melosys.service.dokument.brev.mapper

import io.getunleash.Unleash
import no.nav.melosys.domain.avgift.AvgiftspliktigPeriode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdeavgiftmottaker
import no.nav.melosys.featuretoggle.ToggleName
import no.nav.melosys.integrasjon.dokgen.dto.Avgiftsgrunnlag
import org.springframework.stereotype.Component

/**
 * Skatteforhold og inntekter slik de vises på trygdeavgift-steget i melosys-web, til vedlegget «Avgiftsgrunnlag».
 */
@Component
class AvgiftsgrunnlagMapper(private val unleash: Unleash) {

    fun mapNårAvgiftBetalesTilNav(
        trygdeavgiftsperioder: Collection<Trygdeavgiftsperiode>,
        trygdeavgiftmottaker: Trygdeavgiftmottaker?,
    ): Avgiftsgrunnlag? =
        if (trygdeavgiftmottaker in MOTTAKERE_MED_AVGIFT_TIL_NAV) map(trygdeavgiftsperioder) else null

    private fun map(trygdeavgiftsperioder: Collection<Trygdeavgiftsperiode>): Avgiftsgrunnlag? {
        if (!unleash.isEnabled(ToggleName.MELOSYS_VEDLEGG_AVGIFTSGRUNNLAG)) return null

        val grunnlag = trygdeavgiftsperioder.flatMap { it.grunnlag() }
        val skatteforhold = grunnlag.mapNotNull { it.skatteforhold }.distinct()
        val inntektsperioder = grunnlag.mapNotNull { it.inntektsperiode }.distinct()
        if (skatteforhold.isEmpty() && inntektsperioder.isEmpty()) return null

        val erEøsPensjonist = trygdeavgiftsperioder.any { it.grunnlagHelseutgiftDekkesPeriode != null }
        val erPliktigMedlemskap = grunnlag.mapNotNull { it.avgiftspliktigPeriode }.all { it.erPliktigMedlemskap() }
        val erSkattepliktigIHelePerioden = skatteforhold.none { it.skatteplikttype == Skatteplikttype.IKKE_SKATTEPLIKTIG }

        return Avgiftsgrunnlag(
            skatteforholdsperioder = skatteforhold
                .map { Avgiftsgrunnlag.Skatteforholdsperiode(it.fomDato, it.tomDato, it.skatteplikttype) }
                .distinct()
                .sortedWith(compareBy({ it.fom }, { it.tom })),
            inntektsperioder = inntektsperioder
                .map { inntektsperiode ->
                    val arbeidsgiversavgiftBetalesTilSkatt = inntektsperiode.isArbeidsgiversavgiftBetalesTilSkatt
                        .takeIf { !erEøsPensjonist && arbeidsgiversavgiftErRelevant(inntektsperiode.type, erPliktigMedlemskap) }
                    val avgiftspliktigInntekt = (inntektsperiode.avgiftspliktigMndInntekt ?: inntektsperiode.avgiftspliktigTotalinntekt)?.verdi
                        .takeIf { bruttoinntektErRelevant(inntektsperiode.type, arbeidsgiversavgiftBetalesTilSkatt, erSkattepliktigIHelePerioden) }
                    Avgiftsgrunnlag.Inntektsperiode(
                        fom = inntektsperiode.fomDato,
                        tom = inntektsperiode.tomDato,
                        inntektskildetype = inntektsperiode.type,
                        arbeidsgiversavgiftBetalesTilSkatt = arbeidsgiversavgiftBetalesTilSkatt,
                        avgiftspliktigInntekt = avgiftspliktigInntekt,
                        erMaanedsbelop = inntektsperiode.erMaanedsbelop(),
                    )
                }
                .distinct()
                .sortedWith(compareBy({ it.fom }, { it.tom })),
            visArbeidsgiversavgift = !erEøsPensjonist,
        )
    }

    private fun Trygdeavgiftsperiode.grunnlag(): List<Grunnlag> =
        if (grunnlagListe.isNotEmpty()) {
            grunnlagListe.map { Grunnlag(it.skatteforhold, it.inntektsperiode, it.hentAvgiftspliktigperiode()) }
        } else {
            listOf(
                Grunnlag(
                    grunnlagSkatteforholdTilNorge,
                    grunnlagInntekstperiode,
                    grunnlagMedlemskapsperiode ?: grunnlagHelseutgiftDekkesPeriode ?: grunnlagLovvalgsPeriode
                )
            )
        }

    private class Grunnlag(
        val skatteforhold: SkatteforholdTilNorge?,
        val inntektsperiode: Inntektsperiode?,
        val avgiftspliktigPeriode: AvgiftspliktigPeriode?,
    )

    /** Samme regel som trygdeavgift-steget i melosys-web (arbAvgBetalesKreves). */
    private fun arbeidsgiversavgiftErRelevant(inntektskildetype: Inntektskildetype, erPliktigMedlemskap: Boolean): Boolean =
        !erPliktigMedlemskap && inntektskildetype != Inntektskildetype.MISJONÆR

    /** Samme regel som trygdeavgift-steget i melosys-web (bruttoInntektKreves). */
    private fun bruttoinntektErRelevant(
        inntektskildetype: Inntektskildetype,
        arbeidsgiversavgiftBetalesTilSkatt: Boolean?,
        erSkattepliktigIHelePerioden: Boolean,
    ): Boolean =
        !erSkattepliktigIHelePerioden ||
            inntektskildetype in INNTEKTSKILDETYPER_MED_BRUTTOINNTEKT ||
            (inntektskildetype in INNTEKTSKILDETYPER_MED_BRUTTOINNTEKT_UTEN_ARBEIDSGIVERSAVGIFT && arbeidsgiversavgiftBetalesTilSkatt == false)

    private companion object {
        val MOTTAKERE_MED_AVGIFT_TIL_NAV = setOf(
            Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV,
            Trygdeavgiftmottaker.TRYGDEAVGIFT_BETALES_TIL_NAV_OG_SKATT,
        )
        val INNTEKTSKILDETYPER_MED_BRUTTOINNTEKT = setOf(
            Inntektskildetype.NÆRINGSINNTEKT_FRA_NORGE,
            Inntektskildetype.FN_SKATTEFRITAK,
            Inntektskildetype.PENSJON_UFØRETRYGD,
        )
        val INNTEKTSKILDETYPER_MED_BRUTTOINNTEKT_UTEN_ARBEIDSGIVERSAVGIFT = setOf(
            Inntektskildetype.INNTEKT_FRA_UTLANDET,
            Inntektskildetype.PENSJON_UFØRETRYGD_KILDESKATT,
        )
    }
}
