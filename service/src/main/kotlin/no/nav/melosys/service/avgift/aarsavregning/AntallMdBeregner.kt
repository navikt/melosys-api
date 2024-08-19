package no.nav.melosys.service.avgift.aarsavregning

import mu.KotlinLogging
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger { }

class AntallMdBeregner(private val fom: LocalDate, private val tom: LocalDate) {

    private val erSammeMånedOgÅr = fom.year == tom.year && fom.monthValue == tom.monthValue
    private val antallDagerFørsteMåned = fom.lengthOfMonth().toBigDecimal()
    private val antallDagerSisteMåned = tom.lengthOfMonth().toBigDecimal()

    fun beregn(): BigDecimal {
        val totalAntall = beregnFørsteMånedProsent() + beregnMånederMellomProsent() + beregnSisteMånedProsent()
        log.debug { "beregner for fom: $fom og tom: $tom som gir total antall: $totalAntall" }
        return totalAntall
    }

    private fun beregnFørsteMånedProsent(): BigDecimal {
        return if (erSammeMånedOgÅr) {
            (tom.dayOfMonth.toBigDecimal() - fom.dayOfMonth.toBigDecimal() + BigDecimal.ONE)
                .divide(antallDagerFørsteMåned, 2, RoundingMode.HALF_UP)
        } else {
            (antallDagerFørsteMåned - fom.dayOfMonth.toBigDecimal() + BigDecimal.ONE)
                .divide(antallDagerFørsteMåned, 2, RoundingMode.HALF_UP)
        }
    }

    private fun beregnMånederMellomProsent(): BigDecimal {
        return if (erSammeMånedOgÅr) {
            BigDecimal.ZERO
        } else {
            val start = fom.withDayOfMonth(1).plusMonths(1)
            val end = tom.withDayOfMonth(1)
            ChronoUnit.MONTHS.between(start, end).toBigDecimal().setScale(2, RoundingMode.HALF_UP)
        }
    }

    private fun beregnSisteMånedProsent(): BigDecimal {
        return if (erSammeMånedOgÅr) {
            BigDecimal.ZERO
        } else {
            tom.dayOfMonth.toBigDecimal().divide(antallDagerSisteMåned, 2, RoundingMode.HALF_UP)
        }
    }
}
