package no.nav.melosys.integrasjon.trygdeavgift.dto

import com.google.common.collect.BiMap
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.kodeverk.Avgiftsdekning
import no.nav.melosys.domain.kodeverk.Avgiftsdekning.*
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.Trygdedekninger.*
import no.nav.melosys.exception.FunksjonellException
import java.util.*


data class TrygdeavgiftBeregningsgrunnlagDto(
    val medlemskapsperioder: Set<MedlemskapsperiodeDto>,
    val skatteforholdsperioder: Set<SkatteforholdsperiodeDto>,
    val inntektsperioder: List<InntektsperiodeDto>
) {

    companion object {
        fun av(
            medlemskapsperioder: Collection<Medlemskapsperiode>,
            skatteforholdTilNorge: Collection<SkatteforholdTilNorge>,
            inntektsperioder: Collection<Inntektsperiode>,
            DBID_UUID_MAP: MutableList<BiMap<Long, UUID>>
        ): TrygdeavgiftBeregningsgrunnlagDto =
            TrygdeavgiftBeregningsgrunnlagDto(
                mapMedlemskapsperioder(medlemskapsperioder, DBID_UUID_MAP).toSet(),
                mapSkatteforholdsperioder(skatteforholdTilNorge, DBID_UUID_MAP).toSet(),
                mapInntektsperioder(inntektsperioder, DBID_UUID_MAP)
            )

        private fun mapMedlemskapsperioder(
            medlemskapsperioder: Collection<Medlemskapsperiode>,
            DBID_UUID_MAP: MutableList<BiMap<Long, UUID>>
        ): List<MedlemskapsperiodeDto> =
            medlemskapsperioder.map {
                MedlemskapsperiodeDto(
                    DBID_UUID_MAP.get(0).put(it.id, UUID.randomUUID())!!,
                    DatoPeriodeDto(it.fom, it.tom),
                    avgiftsdekningerFraTrygdedekning(it.trygdedekning)
                )
            }

        private fun mapSkatteforholdsperioder(
            skatteforholdTilNorge: Collection<SkatteforholdTilNorge>,
            DBID_UUID_MAP: MutableList<BiMap<Long, UUID>>
        ): List<SkatteforholdsperiodeDto> =
            skatteforholdTilNorge.map {
                SkatteforholdsperiodeDto(
                    DBID_UUID_MAP.get(1).put(it.id, UUID.randomUUID())!!,
                    DatoPeriodeDto(it.fomDato, it.tomDato),
                    it.skatteplikttype
                )
            }

        private fun mapInntektsperioder(
            inntektsperioder: Collection<Inntektsperiode>,
            DBID_UUID_MAP: MutableList<BiMap<Long, UUID>>
        ): List<InntektsperiodeDto> =
            inntektsperioder.map {
                InntektsperiodeDto(
                    DBID_UUID_MAP.get(2).put(it.id, UUID.randomUUID())!!,
                    DatoPeriodeDto(it.fomDato, it.tomDato),
                    it.type,
                    it.isArbeidsgiversavgiftBetalesTilSkatt,
                    it.isTrygdeavgiftBetalesTilSkatt,
                    PengerDto(it.avgiftspliktigInntektMnd)
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
