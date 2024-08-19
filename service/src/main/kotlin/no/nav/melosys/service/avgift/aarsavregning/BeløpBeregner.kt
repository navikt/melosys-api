package no.nav.melosys.service.avgift.aarsavregning

import mu.KotlinLogging
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

private val log = KotlinLogging.logger { }

class BeløpBeregner {
    companion object {
        fun totalBeløpForAllePerioder(belopsperiode: List<Belopsperiode>): BigDecimal {
            return belopsperiode.sumOf { periode ->
                beløpForPeriode(
                    enhetspris = periode.enhetspris,
                    fom = periode.fom,
                    tom = periode.tom
                )
            }
        }

        fun beløpForPeriode(enhetspris: BigDecimal, fom: LocalDate, tom: LocalDate): BigDecimal {
            val antallMåneder = AntallMdBeregner(fom, tom).beregn()
            val beløp = enhetspris.multiply(antallMåneder).setScale(2, RoundingMode.UNNECESSARY)
            log.debug { "Beløp for periode fom: $fom, tom: $tom regnes med enhetspris $enhetspris og antall: $antallMåneder ==> beløp: $beløp" }
            return beløp
        }

    }
}

data class Belopsperiode(val enhetspris: BigDecimal, val fom: LocalDate, val tom: LocalDate)
