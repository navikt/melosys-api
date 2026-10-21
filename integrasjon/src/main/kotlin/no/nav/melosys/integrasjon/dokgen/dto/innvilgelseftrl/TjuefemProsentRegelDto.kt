package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.melosys.domain.avgift.Avgiftsberegningsregel
import no.nav.melosys.domain.avgift.Avgiftsdel
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import java.math.BigDecimal

data class TjuefemProsentRegelDto(
    val avgiftssatser: List<AvgiftssatsDto>,
    val avgiftPerAvgiftsdel: List<AvgiftPerAvgiftsdelDto>,
)

data class AvgiftssatsDto(
    @JsonProperty("år")
    val år: Int,
    val avgiftssats: BigDecimal?,
    val beregningsregel: Avgiftsberegningsregel,
    val inntektskildetype: Inntektskildetype?,
    val harSammenslåtteInntektskilder: Boolean,
    val trygdedekning: String,
    val avgiftsdel: Avgiftsdel?,
)

data class AvgiftPerAvgiftsdelDto(
    @JsonProperty("år")
    val år: Int,
    val avgiftsdel: Avgiftsdel?,
    val beregningsregel: Avgiftsberegningsregel,
    val avgiftPerMd: BigDecimal,
)

data class ÅrsinntektDto(
    @JsonProperty("år")
    val år: Int,
    @JsonProperty("årsinntekt")
    val årsinntekt: BigDecimal,
)

data class OrdinærSatsPåfølgendeÅrDto(
    val etterÅr: Int,
    @JsonProperty("år")
    val år: Int,
    val avgiftssatser: List<AvgiftssatsDto>,
)
