package no.nav.melosys.melosysmock.journalpost.journalpostapi

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpprettJournalpostRequest(
    val journalpostType: JournalpostType? = null,
    val avsenderMottaker: AvsenderMottaker? = null,
    val bruker: Bruker? = null,
    val datoMottatt: LocalDate? = null,
    val tema: String? = null,
    val behandlingstema: String? = null,
    val tittel: String? = null,
    val kanal: String? = null,
    val journalfoerendeEnhet: String? = null,
    val eksternReferanseId: String? = null,
    val tilleggsopplysninger: List<TilleggsopplysningReq> = ArrayList(),
    val sak: Sak? = null,
    val dokumenter: List<Dokument>? = null
)

data class OppdaterJournalpostRequest(
    val journalpostType: JournalpostType? = null,
    val avsenderMottaker: AvsenderMottaker? = null,
    val bruker: Bruker? = null,
    val datoMottatt: LocalDate? = null,
    val tema: String? = null,
    val behandlingstema: String? = null,
    val tittel: String? = null,
    val kanal: String? = null,
    val journalfoerendeEnhet: String? = null,
    val eksternReferanseId: String? = null,
    val tilleggsopplysninger: List<TilleggsopplysningReq> = ArrayList(),
    val sak: Sak? = null,
    val dokumenter: List<DokumentOppdatering>? = null,
)

data class FerdigstillJournalpostRequest(
    val journalfoerendeEnhet: String? = null
)

enum class JournalpostType {
    INNGAAENDE, UTGAAENDE, NOTAT
}

data class AvsenderMottaker(
    val id: String? = null,
    val navn: String? = null,
    val land: String? = null,
    val idType: IdType? = null,
)

enum class IdType {
    FNR, ORGNR, HPRNR, UTL_ORG
}

data class Bruker(
    val idType: BrukerIdType? = null,
    val id: String? = null
)

enum class BrukerIdType {
    FNR, ORGNR
}

data class TilleggsopplysningReq(
    val nokkel: String? = null,
    val verdi: String? = null
)

data class Sak(
    val fagsakId: String? = null,
    val sakstype: String? = null,
    val fagsaksystem: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Dokument(
    val tittel: String? = null,
    val brevkode: String? = null,
    val dokumentKategori: String? = null,
    val dokumentvarianter: List<DokumentVariant>? = null
)

data class DokumentOppdatering(
    val dokumentInfoId: String,
    val tittel: String? = null,
    val brevkode: String? = null
)

data class DokumentVariant(
    val filtype: JournalpostFiltype = JournalpostFiltype.PDFA,
    val variantformat: String? = null,
    val fysiskDokument: ByteArray? = null
)

enum class JournalpostFiltype {
    PDF, PDFA, XML, RTF, DLF, JPEG,
    TIFF, AXML, DXML, JSON, PNG
}
