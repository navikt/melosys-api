package no.nav.melosys.integrasjon.dokgen.dto

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.domain.brev.ÅrsavregningVedtakBrevBestilling
import no.nav.melosys.domain.kodeverk.Mottakerroller
import java.math.BigDecimal
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
class ÅrsavregningVedtaksbrev(
    brevBestilling: ÅrsavregningVedtakBrevBestilling,
    val årsavregningsår: Int,
    val endeligTrygdeavgift: List<Avgiftsperiode>,
    val forskuddsvisFakturertTrygdeavgift: List<Avgiftsperiode>,
    val endeligTrygdeavgiftTotalbeløp: BigDecimal,
    val forskuddsvisFakturertTrygdeavgiftTotalbeløp: BigDecimal,
    val differansebeløp: BigDecimal,
    val minimumsbeløpForFakturering: BigDecimal,
    val harGrunnlagKunFraMelosys: Boolean,
    val innledningFritekst: String?,
    val begrunnelseFritekst: String?,
    val pliktigMedlemskap: Boolean,
    val eøsEllerTrygdeavtale: Boolean,
    val fullmektigTrygdeavgift: String?
) : DokgenDto(brevBestilling, Mottakerroller.BRUKER) {
    constructor(
        brevBestilling: ÅrsavregningVedtakBrevBestilling,
        årsavregningsår: Int,
        endeligTrygdeavgift: List<Avgiftsperiode>,
        forskuddsvisFakturertTrygdeavgift: List<Avgiftsperiode>,
        endeligTrygdeavgiftTotalbeløp: BigDecimal,
        forskuddsvisFakturertTrygdeavgiftTotalbeløp: BigDecimal,
        differansebeløp: BigDecimal,
        minimumsbeløpForFakturering: BigDecimal,
        harGrunnlagKunFraMelosys: Boolean,
        pliktigMedlemskap: Boolean,
        eøsEllerTrygdeavtale: Boolean,
        fullmektigTrygdeavgift: String?
    ) : this(
        brevBestilling = brevBestilling,
        årsavregningsår = årsavregningsår,
        endeligTrygdeavgift = endeligTrygdeavgift,
        forskuddsvisFakturertTrygdeavgift = forskuddsvisFakturertTrygdeavgift,
        endeligTrygdeavgiftTotalbeløp = endeligTrygdeavgiftTotalbeløp,
        forskuddsvisFakturertTrygdeavgiftTotalbeløp = forskuddsvisFakturertTrygdeavgiftTotalbeløp,
        differansebeløp = differansebeløp,
        minimumsbeløpForFakturering = minimumsbeløpForFakturering,
        harGrunnlagKunFraMelosys = harGrunnlagKunFraMelosys,
        innledningFritekst = brevBestilling.innledningFritekstAarsavregning,
        begrunnelseFritekst = brevBestilling.begrunnelseFritekstAarsavregning,
        pliktigMedlemskap = pliktigMedlemskap,
        eøsEllerTrygdeavtale = eøsEllerTrygdeavtale,
        fullmektigTrygdeavgift = fullmektigTrygdeavgift
    )
}

data class Avgiftsperiode(
    val fom: LocalDate,
    val tom: LocalDate,
    val avgiftssats: BigDecimal,
    val avgiftPerMd: BigDecimal,
    val avgiftspliktigInntektPerMd: BigDecimal,
    val inntektskilde: String,
    val trygdedekning: String,
    val arbeidsgiveravgiftBetalt: SvarAlternativ,
    val skatteplikt: Boolean
)


