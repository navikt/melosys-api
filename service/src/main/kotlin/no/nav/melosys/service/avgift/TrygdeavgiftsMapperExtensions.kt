package no.nav.melosys.service.avgift

import no.nav.melosys.domain.Medlemskapsperiode
import no.nav.melosys.domain.avgift.Inntektsperiode
import no.nav.melosys.domain.avgift.SkatteforholdTilNorge
import no.nav.melosys.integrasjon.trygdeavgift.AvgiftsdekningerFraTrygdedekning
import no.nav.melosys.integrasjon.trygdeavgift.dto.*
import no.nav.melosys.service.avgift.aarsavregning.totalbeloep.TotalBeløpBeregner

import java.util.*

object TrygdeavgiftsMapperExtensions {
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
        val mndsBelop = if (isErMaanedsbelop) {
            PengerDto(avgiftspliktigInntekt)
        } else {
            val kalkulertBelop = TotalBeløpBeregner.månedligBeløpForTotalbeløp(fomDato, tomDato, avgiftspliktigInntekt.verdi)
            PengerDto(kalkulertBelop)
        }

        return InntektsperiodeDto(
            id,
            DatoPeriodeDto(fomDato, tomDato),
            type,
            isArbeidsgiversavgiftBetalesTilSkatt,
            mndsBelop,
            true
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
}
