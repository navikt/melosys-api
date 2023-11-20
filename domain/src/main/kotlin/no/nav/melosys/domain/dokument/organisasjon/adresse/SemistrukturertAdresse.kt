package no.nav.melosys.domain.dokument.organisasjon.adresse

import com.fasterxml.jackson.annotation.JsonView
import no.nav.melosys.domain.dokument.DokumentView

class SemistrukturertAdresse : GeografiskAdresse() {
    var adresselinje1: String? = null
    var adresselinje2: String? = null
    var adresselinje3: String? = null
    var postnr: String? = null
    var poststed: String? = null
    var kommunenr: String? = null

    @JsonView(DokumentView.Database::class)
    var poststedUtland: String? = null
}
