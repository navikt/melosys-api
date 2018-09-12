package no.nav.melosys.service.journalforing.dto;

import java.util.List;

public class JournalfoeringDto {
    private String journalpostID;
    private String oppgaveID;
    private String brukerID;
    private String avsenderID;
    private String avsenderNavn;
    private String dokumentID;
    private String dokumenttittel;
    private List<String> vedleggstitler;

    public String getJournalpostID() {
        return journalpostID;
    }

    public void setJournalpostID(String journalpostID) {
        this.journalpostID = journalpostID;
    }

    public String getOppgaveID() {
        return oppgaveID;
    }

    public void setOppgaveID(String oppgaveID) {
        this.oppgaveID = oppgaveID;
    }

    public String getBrukerID() {
        return brukerID;
    }

    public void setBrukerID(String brukerID) {
        this.brukerID = brukerID;
    }

    public String getAvsenderID() {
        return avsenderID;
    }

    public void setAvsenderID(String avsenderID) {
        this.avsenderID = avsenderID;
    }

    public String getAvsenderNavn() {
        return avsenderNavn;
    }

    public void setAvsenderNavn(String avsenderNavn) {
        this.avsenderNavn = avsenderNavn;
    }

    public String getDokumentID() {
        return dokumentID;
    }

    public void setDokumentID(String dokumentID) {
        this.dokumentID = dokumentID;
    }

    public String getDokumenttittel() {
        return dokumenttittel;
    }

    public void setDokumenttittel(String dokumenttittel) {
        this.dokumenttittel = dokumenttittel;
    }

    public List<String> getVedleggstitler() {
        return vedleggstitler;
    }

    public void setVedleggstitler(List<String> vedleggstitler) {
        this.vedleggstitler = vedleggstitler;
    }

}
