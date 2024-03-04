package no.nav.melosys.integrasjon.doksys

import no.nav.melosys.domain.UtenlandskMyndighet
import no.nav.melosys.domain.adresse.StrukturertAdresse
import no.nav.melosys.domain.brev.Mottaker

data class DokumentbestillingMetadata(
    var dokumenttypeID: String? = null,
    var brukerID: String? = null,
    var brukerNavn: String? = null,
    var mottaker: Mottaker? = null,
    var mottakerID: String? = null,
    var journalsakID: String? = null,
    var fagområde: String? = null,
    var saksbehandler: String? = null,
    var utenlandskMyndighet: UtenlandskMyndighet? = null,
    var postadresse: StrukturertAdresse? = null,
    var berik: Boolean? = null
)
