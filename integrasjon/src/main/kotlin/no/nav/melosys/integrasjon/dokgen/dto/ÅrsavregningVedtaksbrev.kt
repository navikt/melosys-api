package no.nav.melosys.integrasjon.dokgen.dto

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.domain.brev.ÅrsavregningVedtakBrevBestilling
import no.nav.melosys.domain.kodeverk.Mottakerroller
import java.math.BigDecimal
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
class ÅrsavregningVedtaksbrev(
    brevBestilling: ÅrsavregningVedtakBrevBestilling,
    // FIXME hordkodet data for å kunne merge PR navikt/melosys-dokgen#397 uten å gi feil når vedtaksbrevet brukes i q2
    val årsavregningsår: Int,
    val endeligTrygdeavgift: List<Avgiftsperiode>,
    val forskuddsvisFakturertTrygdeavgift: List<Avgiftsperiode>,
    val endeligTrygdeavgiftTotalbeløp: BigDecimal?,
    val forskuddsvisFakturertTrygdeavgiftTotalbeløp: BigDecimal?,
    val differansebeløp: BigDecimal,
    val minimumsbeløpForFakturering: BigDecimal,
    val innledningFritekst: String?,
    val begrunnelseFritekst: String?,
    val pliktigMedlemskap: Boolean?,
    val eøsEllerTrygdeavtale: Boolean?,
) : DokgenDto(brevBestilling, Mottakerroller.BRUKER) {
    constructor(
        brevBestilling: ÅrsavregningVedtakBrevBestilling,
        årsavregningsår: Int,
        endeligTrygdeavgift: List<Avgiftsperiode>,
        forskuddsvisFakturertTrygdeavgift: List<Avgiftsperiode>,
        endeligTrygdeavgiftTotalbeløp: BigDecimal?,
        forskuddsvisFakturertTrygdeavgiftTotalbeløp: BigDecimal?,
        differansebeløp: BigDecimal,
        minimumsbeløpForFakturering: BigDecimal,
        pliktigMedlemskap: Boolean?,
        eøsEllerTrygdeavtale: Boolean?
    ) : this(
        brevBestilling = brevBestilling,
        årsavregningsår = årsavregningsår,
        endeligTrygdeavgift = endeligTrygdeavgift,
        forskuddsvisFakturertTrygdeavgift = forskuddsvisFakturertTrygdeavgift,
        endeligTrygdeavgiftTotalbeløp = endeligTrygdeavgiftTotalbeløp,
        forskuddsvisFakturertTrygdeavgiftTotalbeløp = forskuddsvisFakturertTrygdeavgiftTotalbeløp,
        differansebeløp = differansebeløp,
        minimumsbeløpForFakturering = minimumsbeløpForFakturering,
        innledningFritekst = brevBestilling.innledningFritekstAarsavregning,
        begrunnelseFritekst = brevBestilling.begrunnelseFritekstAarsavregning,
        pliktigMedlemskap = pliktigMedlemskap,
        eøsEllerTrygdeavtale = eøsEllerTrygdeavtale
    )
}

data class Avgiftsperiode(
    val fom: LocalDate = LocalDate.of(2021, 1, 1),
    val tom: LocalDate = LocalDate.of(2021, 1, 31),
    val avgiftssats: BigDecimal = BigDecimal.valueOf(13.9),
    val avgiftPerMd: BigDecimal = BigDecimal.valueOf(12000.00),
    val avgiftspliktigInntektPerMd: BigDecimal = BigDecimal.valueOf(50000.00),
    val inntektskilde: String = "Donasjon",
    val trygdedekning: String = "Briller",
    val arbeidsgiveravgiftBetalt: Boolean = false,
    val skatteplikt: Boolean = true
)


