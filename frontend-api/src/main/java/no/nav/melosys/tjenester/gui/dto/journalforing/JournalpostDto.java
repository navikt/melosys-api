package no.nav.melosys.tjenester.gui.dto.journalforing;

public class JournalpostDto {
    private String brukerID;
    private String avsenderID;
    private boolean erBrukerAvsender;
    private DokumentDto dokument;

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
