package no.nav.melosys.tjenester.gui.dto.journalforing

import no.nav.melosys.domain.arkiv.ArkivDokument
import no.nav.melosys.domain.arkiv.BrukerIdType
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.domain.kodeverk.Avsendertyper
import no.nav.melosys.tjenester.gui.dto.dokumentarkiv.DokumentDto
import java.time.Instant

class JournalpostDto private constructor(
    val mottattDato: Instant, val brukerID: String?,
    val virksomhetOrgnr: String?, val avsenderID: String?, val avsenderNavn: String,
    val avsenderType: Avsendertyper, val isErHovedpartAvsender: Boolean,
    val hoveddokument: DokumentDto, val vedlegg: List<DokumentDto>
) {
    var behandlingsInformasjon: BehandlingsInformasjon? = null

    companion object {
        @JvmStatic
        fun av(journalpost: Journalpost, hovedpartIdent: String?): JournalpostDto {
            val hovedpartErBruker = journalpost.brukerIdType != BrukerIdType.ORGNR
            val avsenderID = journalpost.avsenderId
            val hoveddokument = DokumentDto(
                journalpost.hoveddokument.dokumentId,
                journalpost.hoveddokument.tittel,
                journalpost.hoveddokument.hentLogiskeVedleggTitler()
            )
            val vedlegg = journalpost.vedleggListe.stream()
                .map { v: ArkivDokument -> DokumentDto(v.dokumentId, v.tittel, v.hentLogiskeVedleggTitler()) }
                .toList()

            return JournalpostDto(
                journalpost.forsendelseMottatt,
                if (hovedpartErBruker) hovedpartIdent else null,
                if (!hovedpartErBruker) hovedpartIdent else null,
                avsenderID,
                journalpost.avsenderNavn,
                journalpost.avsenderType,
                avsenderID != null && avsenderID.equals(hovedpartIdent, ignoreCase = true),
                hoveddokument,
                vedlegg
            )
        }
    }
}
