package no.nav.melosys.integrasjon.trygdeavgift.dto

import no.nav.melosys.domain.avgift.Avgiftsberegningsregel
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Forklaring på hvordan trygdeavgiften ble beregnet for et gitt år/regelgruppe.
 *
 * Speiler eksakt kontrakten fra melosys-trygdeavgift-beregning sin
 * [BeregnetTrygdeavgiftResponse]. Feltnavn/casing er ASCII og må ikke endres.
 * Alle beløp er hele kroner (Int). Forklaringen persisteres ikke i melosys-api
 * og føres kun gjennom til frontend på PUT-veien (beregning).
 */
data class BeregningsforklaringDto(
    val aar: Int,
    val regelgruppe: Regelgruppe,
    val valgtRegel: Avgiftsberegningsregel,
    val aarsak: Beregningsaarsak,
    val inntektsgrunnlag: List<InntektslinjeDto>,
    val ekskluderteInntekter: List<EkskludertInntektslinjeDto>,
    val sumAarligInntekt: Int,
    val minstebeloep: Int,
    val inntektOverMinstebeloep: Int?,
    val maksimalAvgift25Prosent: Int?,
    val ordinaerAvgift: Int,
    val ordinaerAvgiftLinjer: List<OrdinaerAvgiftslinjeDto> = emptyList(),
    val fastsattAvgift: Int,
    val fastsattAvgiftPerMaaned: Int = 0,
)

data class InntektslinjeDto(
    val inntektskilde: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val maanedsbeloep: Int,
    val antallMaaneder: Int,
    val sumBeloep: Int,
)

/**
 * Én linje i utregningen av ordinær avgift (grunnlag × sats). Summen av [beloep]
 * utgjør [BeregningsforklaringDto.ordinaerAvgift]. [sats] er prosentsats (f.eks. 7.7).
 */
data class OrdinaerAvgiftslinjeDto(
    val inntektskilde: String,
    val grunnlag: Int,
    val sats: BigDecimal,
    val beloep: Int,
)

data class EkskludertInntektslinjeDto(
    val inntektskilde: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val sumBeloep: Int,
    val aarsak: Ekskluderingsaarsak,
)

enum class Regelgruppe {
    SAMLET,
    HELSEDEL,
    PENSJONSDEL,
    MISJONAER,
}

enum class Beregningsaarsak {
    BEREGNET,
    INNTEKT_UNDER_MINSTEBELØP,
    INGEN_INNTEKT,
}

enum class Ekskluderingsaarsak {
    SKATTEETATEN_FASTSETTER,
}
