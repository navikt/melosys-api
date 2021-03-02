package no.nav.melosys.tjenester.gui.dto.dokumentarkiv;

import java.time.Instant;

import no.nav.melosys.domain.arkiv.ArkivDokument;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.Journalposttype;
import no.nav.melosys.domain.kodeverk.Mottaksretning;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JournalpostInfoDtoTest {

    @Test
    public void av() {
        String journalpostID = "journalpostID";
        Journalpost journalpost = new Journalpost(journalpostID);
        journalpost.setJournalposttype(Journalposttype.INN);
        Instant nå = Instant.now();
        journalpost.setForsendelseMottatt(nå);
        journalpost.setForsendelseJournalfoert(nå);
        ArkivDokument hoveddokument = new ArkivDokument();
        String hovedTittel = "tittel";
        hoveddokument.setTittel(hovedTittel);
        hoveddokument.setDokumentId("1");
        journalpost.setHoveddokument(hoveddokument);
        String partNavn = "part navn";
        journalpost.setKorrespondansepartNavn(partNavn);

        ArkivDokument v = new ArkivDokument();
        String vedleggsTittel = "tittelV";
        v.setTittel(vedleggsTittel);
        v.setDokumentId("2");
        journalpost.getVedleggListe().add(v);

        JournalpostInfoDto dto = JournalpostInfoDto.av(journalpost);

        assertThat(dto.journalpostID).isEqualTo(journalpostID);
        assertThat(dto.mottaksretning).isEqualTo(Mottaksretning.INN);
        assertThat(dto.mottattDato).isNotNull();
        assertThat(dto.journalforingDato).isNotNull();
        assertThat(dto.avsenderEllerMottaker).isEqualTo(partNavn);
        assertThat(dto.hoveddokument.dokumentID).isEqualTo("1");
        assertThat(dto.hoveddokument.tittel).isEqualTo(hovedTittel);
        assertThat(dto.vedlegg.get(0).dokumentID).isEqualTo("2");
        assertThat(dto.vedlegg.get(0).tittel).isEqualTo(vedleggsTittel);
    }
}