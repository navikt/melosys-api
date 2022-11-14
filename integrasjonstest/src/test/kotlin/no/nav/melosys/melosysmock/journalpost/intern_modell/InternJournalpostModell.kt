package no.nav.melosys.melosysmock.journalpost.intern_modell

import java.time.LocalDate
import java.time.LocalDateTime

data class JournalpostModell(
    var journalpostId: String,
    var avsenderMottaker: AvsenderMottaker,
    var dokumentModellList: List<DokumentModell> = mutableListOf(),
    var sakId: String? = null,
    var sakstype: String? = null,
    var journalStatus: JournalStatus? = null,
    var mottattDato: LocalDate? = null,
    var arkivtema: Tema? = null,
    var journalposttype: Journalposttype? = null,
    var bruker: JournalpostBruker? = null,
    var kanal: String? = null,
    var tittel: String? = null,
    var eksternReferanseId: String? = null,
    var journalfoerendeEnhet: String? = null,
    var journalfoertDato: LocalDateTime? = null,
    val tilleggsoppltsninger: MutableSet<Tilleggsopplysning> = mutableSetOf()
) {
    fun validerKanFerdigstilles() {
        if (journalStatus == JournalStatus.J) {
            throw IllegalStateException("Journalpost er allerede ferdigstilt")
        }
        if (sakId == null && sakstype == null) {
            throw IllegalStateException("SakId og sakstype er påkrevd")
        }
        if (tittel == null) {
            throw IllegalStateException("Tittel er på krevd")
        }
        if (avsenderMottaker.id != null && avsenderMottaker.type == null) {
            throw IllegalStateException("Avsendermottaker type er påkrevd dersom id er satt")
        }
        if (journalposttype == Journalposttype.INNGAAENDE && kanal == null) {
            throw IllegalStateException("Mottakskanal er påkrevd")
        }
        if (bruker == null || bruker!!.brukerType == null || bruker!!.ident == null) {
            throw IllegalStateException("Bruker er påkrevd")
        }
    }
}



data class DokumentModell(
    var dokumentId: String? = null,
    var dokumentType: String? = null,
    var erSensitiv: Boolean? = null,
    var tittel: String? = null,
    var brevkode: String? = null,
    var dokumentTilknyttetJournalpost: DokumentTilknyttetJournalpost? = null,
    var dokumentVarianter: List<DokumentVariantInnhold>? = mutableListOf(),
    var dokumentkategori: String? = null
) {
    fun harVariantArkiv(): Boolean {
        return dokumentVarianter?.any { it.variantFormat == VariantFormat.ARKIV } ?: false
    }
}

data class AvsenderMottaker(
    var id: String? = null,
    var type: IdType? = null,
    var navn: String? = null,
    var land: String? = null
)

data class JournalpostBruker(
    var ident: String? = null,
    var brukerType: IdType? = null
)

class DokumentVariantInnhold(
    var filType: Arkivfiltype,
    var variantFormat: VariantFormat? = null,
    var dokumentInnhold: ByteArray
)

data class Tilleggsopplysning (
    var nokkel: String? = null,
    var verdi: String? = null
)

enum class JournalStatus {
    J, MO, M, A
}

enum class Tema {
    MED, UFM
}

enum class Journalposttype {
    INNGAAENDE, UTGAAENDE, NOTAT
}

enum class IdType {
    FNR, ORGNR, AKTOERID, UTL_ORG
}

enum class DokumentTilknyttetJournalpost {
    HOVEDDOKUMENT, VEDLEGG
}

enum class Arkivfiltype {
    PDF, PDFA, XML, RTF, AFP, META, DLF,
    JPEG, TIFF, DOC, DOCX, XLS, XLSX, AXML, DXML, JSON, PNG
}

enum class VariantFormat {
    ARKIV, FULLVERSJON, ORIGINAL
}
