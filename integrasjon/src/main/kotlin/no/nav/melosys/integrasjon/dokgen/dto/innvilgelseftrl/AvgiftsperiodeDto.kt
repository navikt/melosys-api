package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl

import tools.jackson.databind.annotation.JsonSerialize
import tools.jackson.databind.ext.javatime.ser.LocalDateSerializer
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import java.math.BigDecimal
import java.time.LocalDate

data class AvgiftsperiodeDto(
    @JsonSerialize(using = LocalDateSerializer::class) val fom: LocalDate,
    @JsonSerialize(using = LocalDateSerializer::class) val tom: LocalDate,
    val avgiftssats: BigDecimal?,
    val avgiftPerMd: BigDecimal,
    val inntektskildetype: Inntektskildetype,
    val avgiftspliktigInntektPerMd: BigDecimal,
    val beregningsregel: String? = null,
)
