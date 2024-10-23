package no.nav.melosys.domain.eessi.melding


data class Adresse(
    var by: String = "",
    var bygning: String? = null,
    var gate: String? = null,
    var land: String? = null,
    var postnummer: String? = null,
    var region: String? = null,
    var type: String? = null
)

