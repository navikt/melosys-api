package no.nav.melosys.service.avgift.aarsavregning.totalbeloep

import mu.KotlinLogging
import no.nav.melosys.domain.avgift.Inntektsperiode
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
            val periodeMedBeløpList = trygdeavgiftsperioder.map {
                PeriodeMedBeløp(
                    fom = it.periodeFra,
                    tom = it.periodeTil,
                    beløp = it.trygdeavgiftsbeløpMd.verdi,
                )
            }
            return totalBeløpForAllePerioder(periodeMedBeløpList)
        }

        fun hentTotalInntekt(trygdeavgiftsperioder: List<Trygdeavgiftsperiode>): BigDecimal {
            val periodeMedBeløpList = trygdeavgiftsperioder.map {
                PeriodeMedBeløp(
                    fom = it.periodeFra,
                    tom = it.periodeTil,
                    beløp = it.grunnlagInntekstperiode.avgiftspliktigInntektMnd.verdi
                )
            }
            return totalBeløpForAllePerioder(periodeMedBeløpList)
        }

        fun hentTotalInntektForInntektkilde(inntektsperiode: Inntektsperiode): BigDecimal {
            return totalBeløpForPeriode(
                fom = inntektsperiode.fomDato,
                tom = inntektsperiode.tomDato,
                beløp = inntektsperiode.avgiftspliktigInntektMnd.verdi
            )
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
    }
}
