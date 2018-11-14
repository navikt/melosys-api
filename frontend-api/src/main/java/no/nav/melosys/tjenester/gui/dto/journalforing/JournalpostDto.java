package no.nav.melosys.tjenester.gui.dto.journalforing;

import java.time.Instant;

import no.nav.melosys.tjenester.gui.dto.dokument.DokumentDto;

public class JournalpostDto {
    private Instant mottattDato;
    private String brukerID;
    private String avsenderID;
    private boolean erBrukerAvsender;
    private DokumentDto dokument;

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

    public DokumentDto getDokument() {
        return dokument;
    }

    public void setDokument(DokumentDto dokument) {
        this.dokument = dokument;
    }
}
