package no.nav.melosys.domain.eessi.sed

data class VedleggReferanse(
    val journalpostId: String,
    val dokumentId: String,
    val tittel: String?
)
