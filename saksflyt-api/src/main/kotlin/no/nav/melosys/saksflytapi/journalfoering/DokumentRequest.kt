package no.nav.melosys.saksflytapi.journalfoering

data class DokumentRequest(
    var dokumentID: String? = null,
    var tittel: String? = null,
    var logiskeVedlegg: MutableList<String> = mutableListOf(),
)

