package no.nav.melosys.tjenester.gui.dto.dokumentarkiv

import no.nav.melosys.domain.arkiv.ArkivDokument
import no.nav.melosys.domain.arkiv.Journalpost
import no.nav.melosys.domain.arkiv.Journalposttype
import no.nav.melosys.domain.kodeverk.Mottaksretning
import java.time.Instant
import java.util.stream.Collectors

class JournalpostInfoDto(
    @JvmField val journalpostID: String,
    @JvmField val mottattDato: Instant?,
    @JvmField val journalforingDato: Instant,
    @JvmField val mottaksretning: Mottaksretning,
    @JvmField val avsenderEllerMottaker: String,
    @JvmField val hoveddokument: DokumentDto,
    @JvmField val vedlegg: List<DokumentDto>
) {
    fun hentGjeldendeTidspunkt(): Instant? {
        return this.mottattDato ?: this.journalforingDato
    }

    companion object {
        @JvmStatic
        fun av(journalpost: Journalpost): JournalpostInfoDto {
            return JournalpostInfoDto(
                journalpost.journalpostId,
                journalpost.forsendelseMottatt,
                journalpost.forsendelseJournalfoert,
                av(journalpost.journalposttype),
                journalpost.korrespondansepartNavn,
                DokumentDto(
                    journalpost.hoveddokument.dokumentId,
                    journalpost.hoveddokument.tittel, journalpost.hoveddokument.hentLogiskeVedleggTitler()
                ),
                lagVedlegg(journalpost.vedleggListe)
            )
        }

        private fun lagVedlegg(vedlegg: List<ArkivDokument>): List<DokumentDto> {
            return vedlegg.stream()
                .map { v: ArkivDokument -> DokumentDto(v.dokumentId, v.tittel, v.hentLogiskeVedleggTitler()) }
                .collect(Collectors.toList())
        }

        @JvmStatic
        fun av(journalposttype: Journalposttype): Mottaksretning {
            val retning = when (journalposttype) {
                Journalposttype.INN -> Mottaksretning.INN
                Journalposttype.UT -> Mottaksretning.UT
                Journalposttype.NOTAT -> Mottaksretning.NOTAT
                else -> throw IllegalArgumentException("Journalposttype " + journalposttype.kode + " støttes ikke.")
            }
            return retning
        }
    }
}
