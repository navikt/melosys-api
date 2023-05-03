package no.nav.melosys.integrasjon.trygdeavgift.dto

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.kodeverk.Avgiftsdekning
import no.nav.melosys.domain.kodeverk.Avgiftsdekning.*
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.Trygdedekninger.*
import no.nav.melosys.exception.FunksjonellException


data class TrygdeavgiftBeregningsgrunnlagDto(
    val medlemskapsperioder: Set<MedlemskapsperiodeDto>,
    val skatteforholdsperioder: Set<SkatteforholdsperiodeDto>,
    val inntektsperioder: List<InntektsperiodeDto>
) {

    companion object {
        fun av(
            medlemskapsperioder: Collection<Medlemskapsperiode>,
            skatteforholdTilNorge: Collection<SkatteforholdTilNorge>,
            inntektsperioder: Collection<Inntektsperiode>
        ): TrygdeavgiftBeregningsgrunnlagDto =
            TrygdeavgiftBeregningsgrunnlagDto(
                mapMedlemskapsperioder(medlemskapsperioder).toSet(),
                mapSkatteforholdsperioder(skatteforholdTilNorge).toSet(),
                mapInntektsperioder(inntektsperioder)
            )


        private fun mapMedlemskapsperioder(medlemskapsperioder: Collection<Medlemskapsperiode>): List<MedlemskapsperiodeDto> =
            medlemskapsperioder.map {
                MedlemskapsperiodeDto(
                    DatoPeriodeDto(it.fom, it.tom),
                    avgiftsdekningerFraTrygdedekning(it.trygdedekning)
                )
            }

        private fun mapSkatteforholdsperioder(skatteforholdTilNorge: Collection<SkatteforholdTilNorge>): List<SkatteforholdsperiodeDto> =
            skatteforholdTilNorge.map {
                SkatteforholdsperiodeDto(
                    DatoPeriodeDto(it.fomDato, it.tomDato),
                    it.skatteplikttype
                )
            }

        private fun mapInntektsperioder(inntektsperioder: Collection<Inntektsperiode>): List<InntektsperiodeDto> =
            inntektsperioder.map {
                InntektsperiodeDto(
                    DatoPeriodeDto(it.fomDato, it.tomDato),
                    it.type,
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
