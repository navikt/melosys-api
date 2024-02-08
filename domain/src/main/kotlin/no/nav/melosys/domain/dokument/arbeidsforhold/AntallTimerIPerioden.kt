package no.nav.melosys.domain.dokument.arbeidsforhold

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.melosys.domain.dokument.felles.Periode
import java.math.BigDecimal
import java.time.YearMonth

data class AntallTimerIPerioden(
    @JsonProperty("timelonnetPeriode")
    var periode: Periode? = null,

    var antallTimer: BigDecimal? = null,

    @JsonProperty("rapporteringsAarMaaned")
    var rapporteringsperiode: YearMonth? = null
)

