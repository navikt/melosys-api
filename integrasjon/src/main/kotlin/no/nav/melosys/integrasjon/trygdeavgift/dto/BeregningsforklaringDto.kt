package no.nav.melosys.integrasjon.trygdeavgift.dto

import no.nav.melosys.domain.avgift.Avgiftsberegningsregel
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Forklaring på hvordan trygdeavgiften ble beregnet for et gitt år/inntektsgruppe.
 *
 * Speiler eksakt kontrakten fra melosys-trygdeavgift-beregning sin
 * [BeregnetTrygdeavgiftResponse]. Feltnavn/casing er ASCII og endres kun ved en
 * koordinert kontraktendring på tvers av alle tre repoer
 * (melosys-trygdeavgift-beregning → melosys-api → melosys-web).
 * Alle beløp er hele kroner (Int). Forklaringen persisteres ikke i melosys-api
 * og føres kun gjennom til frontend på PUT-veien (beregning).
 */
data class BeregningsforklaringDto(
    val aar: Int,
    val inntektsgruppe: Inntektsgruppe,
    val valgtRegel: Avgiftsberegningsregel,
    val aarsak: Beregningsaarsak,
    val inntektsgrunnlag: List<InntektspostDto>,
    val ekskluderteInntekter: List<EkskludertInntektspostDto>,
    val sumAarligInntekt: Int,
    val minstebeloep: Int,
    val inntektOverMinstebeloep: Int?,
    val maksimalAvgift25Prosent: Int?,
    val ordinaerAvgift: Int,
    val ordinaerAvgiftPoster: List<OrdinaerAvgiftspostDto> = emptyList(),
    val fastsattAvgift: Int,
    val fastsattAvgiftPerMaaned: Int = 0,
)

data class InntektspostDto(
    val inntektskilde: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val maanedsbeloep: Int,
    val antallMaaneder: BigDecimal,
    val sumBeloep: Int,
)

/**
 * Én post i utregningen av ordinær avgift (grunnlag × sats). Summen av [beloep]
 * utgjør [BeregningsforklaringDto.ordinaerAvgift]. [sats] er prosentsats (f.eks. 7.7).
 */
data class OrdinaerAvgiftspostDto(
    val inntektskilde: String,
    val grunnlag: Int,
    val sats: BigDecimal,
    val beloep: Int,
)

data class EkskludertInntektspostDto(
    val inntektskilde: String,
    val fom: LocalDate,
    val tom: LocalDate,
    val sumBeloep: Int,
    val aarsak: Ekskluderingsaarsak,
)

enum class Inntektsgruppe {
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
