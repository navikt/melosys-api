package no.nav.melosys.integrasjon.trygdeavgift.dto

import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.kodeverk.Avgiftsdekning
import no.nav.melosys.domain.kodeverk.Avgiftsdekning.*
import no.nav.melosys.domain.kodeverk.Skatteplikttype
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.Trygdedekninger.*
import no.nav.melosys.exception.FunksjonellException


data class TrygdeavgiftBeregningsgrunnlag(
    val medlemskapsperioder: Set<Medlemskapsperiode>,
    val skatteforholdsperioder: Set<Skatteforholdsperiode>,
    val inntektsperioder: List<Inntektsperiode>
) {

    companion object {
        fun av(
            medlemskapsperioder: Collection<no.nav.melosys.domain.Medlemskapsperiode>,
            skatteforholdTilNorge: Collection<SkatteforholdTilNorge>,
            inntektsperioder: Collection<no.nav.melosys.domain.avgift.Inntektsperiode>
        ): TrygdeavgiftBeregningsgrunnlag =
            TrygdeavgiftBeregningsgrunnlag(
                mapMedlemskapsperioder(medlemskapsperioder).toSet(),
                mapSkatteforholdsperioder(skatteforholdTilNorge).toSet(),
                mapInntektsperioder(inntektsperioder)
            )


        private fun mapMedlemskapsperioder(medlemskapsperioder: Collection<no.nav.melosys.domain.Medlemskapsperiode>): List<Medlemskapsperiode> =
            medlemskapsperioder.map {
                Medlemskapsperiode(
                    DatoPeriode(it.fom, it.tom),
                    avgiftsdekningerFraTrygdedekning(it.trygdedekning)
                )
            }

        private fun mapSkatteforholdsperioder(skatteforholdTilNorge: Collection<SkatteforholdTilNorge>): List<Skatteforholdsperiode> =
            skatteforholdTilNorge.map {
                Skatteforholdsperiode(
                    DatoPeriode(it.fomDato, it.tomDato),
                    it.skatteplikttype
                )
            }

        private fun mapInntektsperioder(inntektsperioder: Collection<no.nav.melosys.domain.avgift.Inntektsperiode>): List<Inntektsperiode> =
            inntektsperioder.map {
                Inntektsperiode(
                    DatoPeriode(it.fomDato, it.tomDato),
                    it.type,
                    Skatteplikttype.SKATTEPLIKTIG, // TODO: Fiks etter avklaring.
                    it.isArbeidsgiversavgiftBetalesTilSkatt,
                    it.isTrygdeavgiftBetalesTilSkatt,
                    PengerDto(it.avgiftspliktigInntektMnd),
                )
            }

        private fun avgiftsdekningerFraTrygdedekning(trygdedekning: Trygdedekninger): Set<Avgiftsdekning> {
            return when (trygdedekning) {
                HELSEDEL ->
                    setOf(HELSEDEL_UTEN_SYKEPENGER)
                HELSEDEL_MED_SYKE_OG_FORELDREPENGER ->
                    setOf(HELSEDEL_MED_SYKEPENGER)
                PENSJONSDEL ->
                    setOf(PENSJONSDEL_UTEN_YRKESSKADETRYGD)
                HELSE_OG_PENSJONSDEL ->
                    setOf(HELSEDEL_UTEN_SYKEPENGER, PENSJONSDEL_UTEN_YRKESSKADETRYGD)
                HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER ->
                    setOf(HELSEDEL_MED_SYKEPENGER, PENSJONSDEL_UTEN_YRKESSKADETRYGD)
                else -> throw FunksjonellException("Kan ikke finne avgiftsdekninger fra trygdedekning " + trygdedekning)
            }
        }
    }

}
