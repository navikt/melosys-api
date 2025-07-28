package no.nav.melosys.domain.brev.utkast

class Utkast {
    @JvmRecord
    data class Saksvedlegg(
        val journalpostID: String?,
        val dokumentID: String?
    )

    @JvmRecord
    data class FritekstVedlegg(
        val tittel: String?,
        val fritekst: String?
    )
}
