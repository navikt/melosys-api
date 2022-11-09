package no.nav.melosys.melosysmock.journalpost.journalpostapi

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

@JsonIgnoreProperties(ignoreUnknown = true)
data class OpprettJournalpostRequest(
    @JsonProperty("journalpostType")
    val journalpostType: JournalpostType? = null,
    @JsonProperty("avsenderMottaker")
    val avsenderMottaker: AvsenderMottaker? = null,
    @JsonProperty("bruker")
    val bruker: Bruker? = null,
    @JsonProperty("datoMottatt")
    val datoMottatt: LocalDate? = null,
    @JsonProperty("tema")
    val tema: String? = null,
    @JsonProperty("behandlingstema")
    val behandlingstema: String? = null,
    @JsonProperty("tittel")
    val tittel: String? = null,
    @JsonProperty("kanal")
    val kanal: String? = null,
    @JsonProperty("journalfoerendeEnhet")
    val journalfoerendeEnhet: String? = null,
    @JsonProperty("eksternReferanseId")
    val eksternReferanseId: String? = null,
    @JsonProperty("tilleggsopplysninger")
    val tilleggsopplysninger: List<TilleggsopplysningReq> = ArrayList(),
    @JsonProperty("sak")
    val sak: Sak? = null,
    @JsonProperty("dokumenter")
    val dokumenter: List<Dokument>? = null
)

data class OppdaterJournalpostRequest(
    @JsonProperty("journalpostType")
    val journalpostType: JournalpostType? = null,
    @JsonProperty("avsenderMottaker")
    val avsenderMottaker: AvsenderMottaker? = null,
    @JsonProperty("bruker")
    val bruker: Bruker? = null,
    @JsonProperty("datoMottatt")
    val datoMottatt: LocalDate? = null,
    @JsonProperty("tema")
    val tema: String? = null,
    @JsonProperty("behandlingstema")
    val behandlingstema: String? = null,
    @JsonProperty("tittel")
    val tittel: String? = null,
    @JsonProperty("kanal")
    val kanal: String? = null,
    @JsonProperty("journalfoerendeEnhet")
    val journalfoerendeEnhet: String? = null,
    @JsonProperty("eksternReferanseId")
    val eksternReferanseId: String? = null,
    @JsonProperty("tilleggsopplysninger")
    val tilleggsopplysninger: List<TilleggsopplysningReq>? = ArrayList(),
    @JsonProperty("sak")
    val sak: Sak? = null,
    @JsonProperty("dokumenter")
    val dokumenter: List<DokumentOppdatering>? = null,


    )

data class FerdigstillJournalpostRequest(
    @JsonProperty("journalfoerendeEnhet")
    val journalfoerendeEnhet: String? = null
)

enum class JournalpostType {
    INNGAAENDE, UTGAAENDE, NOTAT
}

data class AvsenderMottaker(
    @JsonProperty("id")
    val id: String? = null,
    @JsonProperty("navn")
    val navn: String? = null,
    @JsonProperty("land")
    val land: String? = null,
    @JsonProperty("idType")
    val idType: IdType? = null,
)

enum class IdType {
    FNR, ORGNR, HPRNR, UTL_ORG
}

data class Bruker(
    @JsonProperty("idType")
    val idType: BrukerIdType? = null,
    @JsonProperty("id")
    val id: String? = null
)

enum class BrukerIdType {
    FNR, ORGNR
}

data class TilleggsopplysningReq(
    @JsonProperty("nokkel")
    val nokkel: String? = null,
    @JsonProperty("verdi")
    val verdi: String? = null
)

data class Sak(
    @JsonProperty("fagsakId")
    val fagsakId: String? = null,
    @JsonProperty("sakstype")
    val sakstype: String? = null,
    @JsonProperty("fagsaksystem")
    val fagsaksystem: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Dokument(
    @JsonProperty("tittel")
    val tittel: String? = null,
    @JsonProperty("brevkode")
    val brevkode: String? = null,
    @JsonProperty("dokumentKategori")
    val dokumentKategori: String? = null,
    @JsonProperty("dokumentvarianter")
    val dokumentvarianter: List<DokumentVariant>? = null
)

data class DokumentOppdatering(
    @JsonProperty("dokumentInfoId")
    val dokumentInfoId: String,
    @JsonProperty("tittel")
    val tittel: String? = null,
    @JsonProperty("brevkode")
    val brevkode: String? = null
)

data class DokumentVariant(
    @JsonProperty("filtype")
    val filtype: JournalpostFiltype = JournalpostFiltype.PDFA,
    @JsonProperty("variantformat")
    val variantformat: String? = null,
    @JsonProperty("fysiskDokument")
    val fysiskDokument: ByteArray? = null
)

enum class JournalpostFiltype {
    PDF, PDFA, XML, RTF, DLF, JPEG,
    TIFF, AXML, DXML, JSON, PNG
}
