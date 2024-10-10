package no.nav.melosys.service.avgift.aarsavregning

import java.math.BigDecimal

enum class ÅrsavregningKonstanter(val beløp: BigDecimal) {
    MINIMUM_BELØP_FAKTURERING(BigDecimal(100))
}
