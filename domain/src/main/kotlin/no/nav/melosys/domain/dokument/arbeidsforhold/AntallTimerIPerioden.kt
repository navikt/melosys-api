package no.nav.melosys.domain.dokument.arbeidsforhold

import no.nav.melosys.domain.dokument.felles.Periode
import java.math.BigDecimal
import java.time.YearMonth

data class AntallTimerIPerioden(
    var antallTimer: BigDecimal? = null,
    var timelonnetPeriode: Periode? = null,
    var rapporteringsAarMaaned: YearMonth? = null
)

