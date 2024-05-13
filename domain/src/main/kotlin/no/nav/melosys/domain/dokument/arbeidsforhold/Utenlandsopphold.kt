package no.nav.melosys.domain.dokument.arbeidsforhold

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.melosys.domain.dokument.felles.Periode
import java.time.YearMonth


class Utenlandsopphold {
    var periode: Periode? = null

    /** Obs. Ikke kodeverk!  */
    var land: String? = null

    @JsonProperty("rapporteringsAarMaaned")
    var rapporteringsperiode: YearMonth? = null
}
