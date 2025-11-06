package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Lovvalgsperiode
import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.AvgiftspliktigPeriode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.domain.helseutgiftdekkesperiode.HelseutgiftDekkesPeriode
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.integrasjon.trygdeavgift.AvgiftsdekningerFraTrygdedekning
import no.nav.melosys.integrasjon.trygdeavgift.dto.*
import no.nav.melosys.service.avgift.aarsavregning.totalbeloep.TotalbeløpBeregner
import java.util.*

fun List<AvgiftspliktigPeriode>.tilAvgiftspliktigperiodeDtoSet(): Set<AvgiftspliktigperiodeDto> {
    return flatMap { periode: AvgiftspliktigPeriode ->
        when (periode) {
            is Medlemskapsperiode -> listOf(periode).tilMedlemskapsperiodeDtoSet()
            is HelseutgiftDekkesPeriode -> listOf(periode).tilHelseutgiftDekkesPeriodeDtoSet()
            is Lovvalgsperiode -> listOf(periode).tilLovvalgsperiodeDtoSet()
            else -> throw FunksjonellException("Ukjent type '${periode.javaClass}'")
        }
    }.toSet()
}

fun List<Medlemskapsperiode>.tilMedlemskapsperiodeDtoSet(): Set<AvgiftspliktigperiodeDto> {
    return map {
        AvgiftspliktigperiodeDto(
            idToUUid(it.hentId()),
            DatoPeriodeDto(it.hentFom(), it.hentTom()),
            AvgiftsdekningerFraTrygdedekning.avgiftsdekningerFraTrygdedekning(it.hentTrygdedekning()),
            it.hentMedlemskapstype()
        )
    }.toSet()
}

fun List<HelseutgiftDekkesPeriode>.tilHelseutgiftDekkesPeriodeDtoSet(): Set<AvgiftspliktigperiodeDto> {
    return map {
        AvgiftspliktigperiodeDto(
            idToUUid(it.hentId()),
            DatoPeriodeDto(it.fomDato, it.tomDato),
            AvgiftsdekningerFraTrygdedekning.avgiftsdekningerFraTrygdedekning(it.hentTrygdedekning()),
            it.hentMedlemskapstype()
        )
    }.toSet()
}

fun List<Lovvalgsperiode>.tilLovvalgsperiodeDtoSet(): Set<AvgiftspliktigperiodeDto> {
    return map {
        AvgiftspliktigperiodeDto(
            idToUUid(it.hentId()),
            DatoPeriodeDto(it.hentFom(), it.hentTom()),
            AvgiftsdekningerFraTrygdedekning.avgiftsdekningerFraTrygdedekningForLovvalg(it.hentTrygdedekning()),
            it.hentMedlemskapstype()
        )
    }.toSet()
}

fun Inntektsperiode.tilInntektsperiodeDto(id: UUID): InntektsperiodeDto {
    val avgiftspliktigMdInntekt = avgiftspliktigMndInntekt ?: avgiftspliktigTotalinntekt?.let { totalinntekt ->
        Penger(
            TotalbeløpBeregner.månedligBeløpForTotalbeløp(
                fomDato,
                tomDato,
                totalinntekt.hentVerdi()
            )
        )
    }

    return InntektsperiodeDto(
        id,
        DatoPeriodeDto(fomDato, tomDato),
        type,
        isArbeidsgiversavgiftBetalesTilSkatt,
        if (avgiftspliktigMdInntekt?.verdi != null) PengerDto(avgiftspliktigMdInntekt) else null
    )
}

fun SkatteforholdTilNorge.tilSkatteforholdDto(id: UUID) = SkatteforholdsperiodeDto(
    id,
    DatoPeriodeDto(fomDato, tomDato),
    skatteplikttype
)

fun HelseutgiftDekkesPeriode.tilHelseutgiftDekkesPeriodeDto(): HelseutgiftDekkesPeriodeDto {
    return HelseutgiftDekkesPeriodeDto(
        DatoPeriodeDto(fomDato, tomDato),
    )
}

fun idToUUid(id: Long): UUID {
    return UUID.nameUUIDFromBytes(id.toString().toByteArray())
}
