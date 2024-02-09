package no.nav.melosys.domain.mottatteopplysninger.data.arbeidssteder

import com.fasterxml.jackson.annotation.JsonProperty
import no.nav.melosys.domain.kodeverk.Innretningstyper
import no.nav.melosys.domain.kodeverk.begrunnelser.Fartsomrader


class MaritimtArbeid {
    var enhetNavn: String? = null
    var fartsomradeKode: Fartsomrader? = null
    var flaggLandkode: String? = null
    var innretningLandkode: String? = null
    var innretningstype: Innretningstyper? = null

    @JsonProperty("territorialfarvann")
    var territorialfarvannLandkode: String? = null
}
