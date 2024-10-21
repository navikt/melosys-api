package no.nav.melosys.service.avgift.aarsavregning.totalbeloep

import mu.KotlinLogging
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

private val log = KotlinLogging.logger { }

class TotalBeløpBeregner {

    companion object {
        fun hentTotalAvgift(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>): BigDecimal? {
            if (trygdeavgiftsperioder.isEmpty()) {
                return null
            }
            val periodeMedBeløpList = trygdeavgiftsperioder.filter { it.grunnlagInntekstperiode != null }.map {
                PeriodeMedBeløp(
                    fom = it.periodeFra,
                    tom = it.periodeTil,
                    beløp = it.trygdeavgiftsbeløpMd.verdi,
                )
            }
            return totalBeløpForAllePerioder(periodeMedBeløpList)
        }

        fun hentTotalInntekt(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>): BigDecimal {
            val periodeMedBeløpList = trygdeavgiftsperioder.filter { it.grunnlagInntekstperiode != null }.map {
                PeriodeMedBeløp(
                    fom = it.periodeFra,
                    tom = it.periodeTil,
                    beløp = it.grunnlagInntekstperiode.avgiftspliktigInntekt.verdi
                )
            }
            return totalBeløpForAllePerioder(periodeMedBeløpList)
        }

        fun totalBeløpForAllePerioder(periodeMedBeløpList: List<PeriodeMedBeløp>): BigDecimal {
            return periodeMedBeløpList.sumOf { periode ->
                totalBeløpForPeriode(
                    fom = periode.fom,
                    tom = periode.tom,
                    beløp = periode.beløp
                )
            }
        }

        fun totalBeløpForPeriode(fom: LocalDate, tom: LocalDate, beløp: BigDecimal): BigDecimal {
            val antallMåneder = AntallMdBeregner(fom, tom).beregn()
            val total = beløp.multiply(antallMåneder).setScale(2, RoundingMode.UNNECESSARY)
            log.debug { "Beløp for periode fom: $fom, tom: $tom regnes med enhetspris $total og antall: $antallMåneder ==> beløp: $total" }
            return total
        }

        // TODO: RoundingError må fikses før merge av 6814
        fun månedligBeløpForTotalbeløp(fom: LocalDate, tom: LocalDate, totalBeløp: BigDecimal): BigDecimal {
            val antallMåneder = AntallMdBeregner(fom, tom).beregn()
            val total = totalBeløp.divide(antallMåneder).setScale(2, RoundingMode.FLOOR)
            return total
        }
    }
}
