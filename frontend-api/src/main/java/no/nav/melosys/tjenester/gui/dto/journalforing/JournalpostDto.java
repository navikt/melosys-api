package no.nav.melosys.tjenester.gui.dto.journalforing;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.tjenester.gui.dto.dokument.DokumentDto;

public class JournalpostDto {
    private Instant mottattDato;
    private String brukerID;
    private String avsenderID;
    private boolean erBrukerAvsender;
    private DokumentDto hoveddokument;
    private List<DokumentDto> vedlegg = new ArrayList<>();

    public Instant getMottattDato() {
        return mottattDato;
    }

    public void setMottattDato(Instant mottattDato) {
        this.mottattDato = mottattDato;
    }

    public String getBrukerID() {
        return brukerID;
    }

    public void setBrukerID(String brukerID) {
        this.brukerID = brukerID;
    }

    public boolean isErBrukerAvsender() {
        return erBrukerAvsender;
    }

    public void setErBrukerAvsender(boolean erBrukerAvsender) {
        this.erBrukerAvsender = erBrukerAvsender;
    }

    public String getAvsenderID() {
        return avsenderID;
    }

    public void setAvsenderID(String avsenderID) {
        this.avsenderID = avsenderID;
    }

    public DokumentDto getHoveddokument() {
        return hoveddokument;
    }

    public void setHoveddokument(DokumentDto hoveddokument) {
        this.hoveddokument = hoveddokument;
    }

    public List<DokumentDto> getVedlegg() {
        return vedlegg;
    }

    public void setVedlegg(List<DokumentDto> vedlegg) {
        this.vedlegg = vedlegg;
    }
}
