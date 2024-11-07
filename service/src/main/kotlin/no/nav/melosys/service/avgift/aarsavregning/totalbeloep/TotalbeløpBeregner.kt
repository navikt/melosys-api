package no.nav.melosys.service.avgift.aarsavregning.totalbeloep

import mu.KotlinLogging
import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

private val log = KotlinLogging.logger { }

object TotalbeløpBeregner {

    fun hentTotalavgift(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>): BigDecimal? {
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
        return totalbeløpForAllePerioder(periodeMedBeløpList)
    }

    fun hentTotalinntekt(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>): BigDecimal {
        val periodeMedBeløpList = trygdeavgiftsperioder.filter { it.grunnlagInntekstperiode != null }.map {
            val mdBelop = (it.grunnlagInntekstperiode.avgiftspliktigMndInntekt ?: it.grunnlagInntekstperiode.avgiftspliktigTotalinntekt).verdi
            PeriodeMedBeløp(
                fom = it.periodeFra,
                tom = it.periodeTil,
                beløp = mdBelop
            )
        }
        return totalbeløpForAllePerioder(periodeMedBeløpList)
    }

    fun totalbeløpForAllePerioder(periodeMedBeløpList: List<PeriodeMedBeløp>): BigDecimal {
        return periodeMedBeløpList.sumOf { periode ->
            totalbeløpForPeriode(
                fom = periode.fom,
                tom = periode.tom,
                beløp = periode.beløp
            )
        }
    }

    fun totalbeløpForPeriode(fom: LocalDate, tom: LocalDate, beløp: BigDecimal): BigDecimal {
        val antallMåneder = AntallMdBeregner(fom, tom).beregn()
        val total = beløp.multiply(antallMåneder).setScale(2, RoundingMode.HALF_UP)
        log.debug { "Beløp for periode fom: $fom, tom: $tom regnes med enhetspris $total og antall: $antallMåneder ==> beløp: $total" }
        return total
    }

    fun månedligBeløpForTotalbeløp(fom: LocalDate, tom: LocalDate, totalBeløp: BigDecimal): BigDecimal {
        val antallMåneder = AntallMdBeregner(fom, tom).beregn()
        val total = totalBeløp.divide(antallMåneder, 2, RoundingMode.HALF_UP)

        return total
    }
}

