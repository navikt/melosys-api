package no.nav.melosys.integrasjon.joark.journalpostapi.dto;

import java.util.Collections;

import no.nav.melosys.domain.arkiv.DokumentVariant;
import no.nav.melosys.domain.arkiv.*;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpprettJournalpostRequestTest {

    @Test
    void av_medGyldigOpprettJournalpost_forventObjekt() {
        FysiskDokument hoveddokument = new FysiskDokument();
        hoveddokument.setTittel("tittel");
        hoveddokument.setBrevkode("brevkode");
        hoveddokument.setDokumentKategori("kategori");
        no.nav.melosys.domain.arkiv.DokumentVariant dokumentVariant = DokumentVariant.lagDokumentVariant("pdf".getBytes());
        hoveddokument.setDokumentVarianter(Collections.singletonList(dokumentVariant));

        OpprettJournalpost opprettJournalpost = new OpprettJournalpost();
        opprettJournalpost.setHoveddokument(hoveddokument);
        opprettJournalpost.setMottaksKanal("S");
        opprettJournalpost.setTema("MED");
        opprettJournalpost.setBrukerId("12345678901");
        opprettJournalpost.setBrukerIdType(BrukerIdType.FOLKEREGISTERIDENT);
        opprettJournalpost.setKorrespondansepartNavn("Trygdemyndighet");
        opprettJournalpost.setKorrespondansepartId("id123");
        opprettJournalpost.setKorrespondansepartIdType(OpprettJournalpost.KorrespondansepartIdType.UTENLANDSK_ORGANISASJON);
        opprettJournalpost.setSaksnummer("MEL-1231");
        opprettJournalpost.setInnhold("Tittel som beskriver innholdet");
        opprettJournalpost.setJournalførendeEnhet("MEDLEMSKAP_OG_AVGIFT");
        opprettJournalpost.setJournalposttype(Journalposttype.UT);

        OpprettJournalpostRequest request = OpprettJournalpostRequest.av(opprettJournalpost);

        assertThat(request).isNotNull();
        assertThat(request.getTittel()).isEqualTo("Tittel som beskriver innholdet");
        assertThat(request.getDokumenter().size()).isEqualTo(1);
        assertThat(request.getKanal()).isEqualTo("S");
    }
}
