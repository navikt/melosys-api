package no.nav.melosys.integrasjon.dokgen.dto.innvilgelseftrl

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import java.math.BigDecimal
import java.time.LocalDate

data class AvgiftsPeriode(
    @JsonSerialize(using = LocalDateSerializer::class) val fom: LocalDate,
    @JsonSerialize(
        using = LocalDateSerializer::class
    ) val tom: LocalDate,
    val avgiftssats: BigDecimal,
    val avgiftPerMd: BigDecimal,
    val inntektskildetype: Inntektskildetype?,
    val avgiftspliktigInntektPerMd: BigDecimal,
)
