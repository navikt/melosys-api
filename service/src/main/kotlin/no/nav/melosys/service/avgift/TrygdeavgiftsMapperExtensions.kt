package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.Penger
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.integrasjon.trygdeavgift.AvgiftsdekningerFraTrygdedekning
import no.nav.melosys.integrasjon.trygdeavgift.dto.*
import no.nav.melosys.service.avgift.aarsavregning.totalbeloep.TotalBeløpBeregner
import java.util.*

fun List<Medlemskapsperiode>.tilMedlemskapsperiodeDtos(): Set<MedlemskapsperiodeDto> {
    return map {
        MedlemskapsperiodeDto(
            idToUUid(it.id),
            DatoPeriodeDto(it.fom, it.tom),
            AvgiftsdekningerFraTrygdedekning.avgiftsdekningerFraTrygdedekning(it.trygdedekning),
            it.medlemskapstype
        )
    }.toSet()
}

fun Inntektsperiode.tilInntektsperiodeDto(id: UUID): InntektsperiodeDto {
    val avgiftspliktigMdInntekt =
        avgiftspliktigInntekt ?: Penger(TotalBeløpBeregner.månedligBeløpForTotalbeløp(fomDato, tomDato, avgiftspliktigTotalinntekt.verdi))

    return InntektsperiodeDto(
        id,
        DatoPeriodeDto(fomDato, tomDato),
        type,
        isArbeidsgiversavgiftBetalesTilSkatt,
        PengerDto(avgiftspliktigMdInntekt)
    )
}

fun SkatteforholdTilNorge.tilSkatteforholdDto(id: UUID) = SkatteforholdsperiodeDto(
    id,
    DatoPeriodeDto(fomDato, tomDato),
    skatteplikttype
)

fun idToUUid(id: Long): UUID {
    return UUID.nameUUIDFromBytes(id.toString().toByteArray())
}
