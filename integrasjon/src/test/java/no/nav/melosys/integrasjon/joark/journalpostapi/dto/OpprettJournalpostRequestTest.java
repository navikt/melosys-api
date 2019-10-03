package no.nav.melosys.integrasjon.joark.journalpostapi.dto;

import no.nav.melosys.domain.arkiv.FysiskDokument;
import no.nav.melosys.domain.arkiv.Journalposttype;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class OpprettJournalpostRequestTest {

    @Test
    public void av_medGyldigOpprettJournalpost_forventObjekt() {
        FysiskDokument hoveddokument = new FysiskDokument();
        hoveddokument.setTittel("tittel");
        hoveddokument.setBrevkode("brevkode");
        hoveddokument.setData("pdf".getBytes());
        hoveddokument.setDokumentKategori("kategori");
        hoveddokument.setVariantFormat("ARKIV");
        hoveddokument.setFiltype(FysiskDokument.Filtype.PDFA);

        OpprettJournalpost opprettJournalpost = new OpprettJournalpost();
        opprettJournalpost.setHoveddokument(hoveddokument);
        opprettJournalpost.setMottaksKanal("S");
        opprettJournalpost.setTema("MED");
        opprettJournalpost.setBrukerId("12345678901");
        opprettJournalpost.setKorrespondansepartNavn("Trygdemyndighet");
        opprettJournalpost.setKorrespondansepartId("id123");
        opprettJournalpost.setArkivSakId("123321");
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