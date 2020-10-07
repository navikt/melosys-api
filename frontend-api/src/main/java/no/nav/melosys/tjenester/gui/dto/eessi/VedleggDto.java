package no.nav.melosys.tjenester.gui.dto.eessi;

public class VedleggDto {
    private String journalpostID;
    private String dokumentID;

    public VedleggDto(String journalpostID, String dokumentID) {
        this.journalpostID = journalpostID;
        this.dokumentID = dokumentID;
    }

    public String getJournalpostID() {
        return journalpostID;
    }

    public void setJournalpostID(String journalpostID) {
        this.journalpostID = journalpostID;
    }

    public String getDokumentID() {
        return dokumentID;
    }

    public void setDokumentID(String dokumentID) {
        this.dokumentID = dokumentID;
    }
}
