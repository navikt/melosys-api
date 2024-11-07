package no.nav.melosys.service.avgift.aarsavregning.totalbeloep

import java.math.BigDecimal
import java.time.LocalDate

data class PeriodeMedBeløp(val fom: LocalDate, val tom: LocalDate, val beløp: BigDecimal)
