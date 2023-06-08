package no.nav.melosys.integrasjon.trygdeavgift

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.kodeverk.Avgiftsdekning
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.trygdeavgift.dto.*
import java.util.*

class TrygdeavgiftsberegningsRequestMapper {

    fun map(
        medlemskapsperioder: Collection<Medlemskapsperiode>,
        skatteforholdTilNorge: Collection<SkatteforholdTilNorge>,
        inntektsperioder: Collection<Inntektsperiode>,
    ): Pair<TrygdeavgiftsberegningRequest, List<Map<UUID, Long>>> {
        val (medlemskapsperioderDto, medlemskapsperiodeMap) = mapMedlemskapsperioder(medlemskapsperioder)
        val (skatteforholdsperioderDto, skatteforholdsperiodeMap) = mapSkatteforholdsperioder(skatteforholdTilNorge)
        val (inntektsperioderDto, inntektsperiodeMap) = mapInntektsperioder(inntektsperioder)
        return Pair(
            TrygdeavgiftsberegningRequest(
                medlemskapsperioderDto,
                skatteforholdsperioderDto,
                inntektsperioderDto
            ),
            listOf(medlemskapsperiodeMap, skatteforholdsperiodeMap, inntektsperiodeMap)
        )
    }

    private fun mapMedlemskapsperioder(medlemskapsperioder: Collection<Medlemskapsperiode>): Pair<Set<MedlemskapsperiodeDto>, Map<UUID, Long>> {
        val map = mutableMapOf<UUID, Long>()
        val perioder = medlemskapsperioder.map {
            val dto = MedlemskapsperiodeDto(
                UUID.randomUUID(),
                DatoPeriodeDto(it.fom, it.tom),
                avgiftsdekningerFraTrygdedekning(it.trygdedekning)
            )
            map[dto.id] = it.id
            dto
        }.toSet()
        return Pair(perioder, map)
    }


    private fun mapSkatteforholdsperioder(skatteforholdTilNorge: Collection<SkatteforholdTilNorge>): Pair<Set<SkatteforholdsperiodeDto>, Map<UUID, Long>> {
        val map = mutableMapOf<UUID, Long>()
        val perioder = skatteforholdTilNorge.map {
            val dto = SkatteforholdsperiodeDto(
                UUID.randomUUID(),
                DatoPeriodeDto(it.fomDato, it.tomDato),
                it.skatteplikttype
            )
            map[dto.id] = it.id
            dto
        }.toSet()
        return Pair(perioder, map)
    }

    private fun mapInntektsperioder(inntektsperioder: Collection<Inntektsperiode>): Pair<List<InntektsperiodeDto>, Map<UUID, Long>> {
        val map = mutableMapOf<UUID, Long>()
        val perioder = inntektsperioder.map {
            val dto = InntektsperiodeDto(
                UUID.randomUUID(),
                DatoPeriodeDto(it.fomDato, it.tomDato),
                it.type,
                it.isArbeidsgiversavgiftBetalesTilSkatt,
                it.isOrdinærTrygdeavgiftBetalesTilSkatt,
                if (it.avgiftspliktigInntektMnd == null) null else PengerDto(it.avgiftspliktigInntektMnd)
            )
            map[dto.id] = it.id
            dto
        }
        return Pair(perioder, map)
    }

    private fun avgiftsdekningerFraTrygdedekning(trygdedekning: Trygdedekninger): Set<Avgiftsdekning> {
        return when (trygdedekning) {
            Trygdedekninger.HELSEDEL ->
                setOf(Avgiftsdekning.HELSEDEL_UTEN_SYKEPENGER)

            Trygdedekninger.HELSEDEL_MED_SYKE_OG_FORELDREPENGER ->
                setOf(Avgiftsdekning.HELSEDEL_MED_SYKEPENGER)

            Trygdedekninger.PENSJONSDEL ->
                setOf(Avgiftsdekning.PENSJONSDEL_UTEN_YRKESSKADETRYGD)

            Trygdedekninger.HELSE_OG_PENSJONSDEL ->
                setOf(Avgiftsdekning.HELSEDEL_UTEN_SYKEPENGER, Avgiftsdekning.PENSJONSDEL_UTEN_YRKESSKADETRYGD)

            Trygdedekninger.HELSE_OG_PENSJONSDEL_MED_SYKE_OG_FORELDREPENGER ->
                setOf(Avgiftsdekning.HELSEDEL_MED_SYKEPENGER, Avgiftsdekning.PENSJONSDEL_UTEN_YRKESSKADETRYGD)

            else -> throw FunksjonellException("Kan ikke finne avgiftsdekninger fra trygdedekning " + trygdedekning)
        }
    }
}
