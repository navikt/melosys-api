package no.nav.melosys.integrasjon.dokgen.dto

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.domain.brev.ÅrsavregningVedtakBrevBestilling
import no.nav.melosys.domain.kodeverk.Mottakerroller
import java.math.BigDecimal
import java.time.LocalDate

@JsonInclude(JsonInclude.Include.NON_NULL)
class ÅrsavregningVedtaksbrev(
    brevBestilling: ÅrsavregningVedtakBrevBestilling,
    val årsavregningsår: Int = 2023,
    val endeligTrygdeavgift: List<Avgiftsperiode> = listOf(Avgiftsperiode()),
    val forskuddsvisFakturertTrygdeavgift: List<Avgiftsperiode> = emptyList(),
    val endeligTrygdeavgiftTotalbeløp: BigDecimal = BigDecimal.TEN,
    val forskuddsvisFakturertTrygdeavgiftTotalbeløp: BigDecimal = BigDecimal.ONE,
    val differansebeløp: BigDecimal = BigDecimal.TEN,
    val minimumsbeløpForFakturering: BigDecimal = BigDecimal.valueOf(100),
    val innledningFritekst: String? = "Dette er en test som ikke bør tas på alvor.",
    val begrunnelseFritekst: String? = null,
    val pliktigMedlemskap: Boolean = false,
    val eøsEllerTrygdeavtale: Boolean = false,
) : DokgenDto(brevBestilling, Mottakerroller.BRUKER) {
    constructor(
        brevBestilling: ÅrsavregningVedtakBrevBestilling,
    ) : this(
        brevBestilling = brevBestilling,
        innledningFritekst = brevBestilling.innledningFritekstAarsavregning,
        begrunnelseFritekst = brevBestilling.begrunnelseFritekstAarsavregning
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


