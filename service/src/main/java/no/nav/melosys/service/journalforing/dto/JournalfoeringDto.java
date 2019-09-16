package no.nav.melosys.service.journalforing.dto;

import java.util.List;

public class JournalfoeringDto {
    private String behandlingstypeKode;
    private String journalpostID;
    private String oppgaveID;
    private String brukerID;
    private String avsenderID;
    private String avsenderNavn;
    private String dokumentID;
    private String hoveddokumentTittel;
    private List<DokumentDto> vedlegg;
    private boolean skalTilordnes;
    private boolean skalSendesForvaltningsmelding;

    public String getBehandlingstypeKode() {
        return behandlingstypeKode;
    }

    public void setBehandlingstypeKode(String behandlingstypeKode) {
        this.behandlingstypeKode = behandlingstypeKode;
    }

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

    public String getHoveddokumentTittel() {
        return hoveddokumentTittel;
    }

    public void setHoveddokumentTittel(String hoveddokumentTittel) {
        this.hoveddokumentTittel = hoveddokumentTittel;
    }

    public List<DokumentDto> getVedlegg() {
        return vedlegg;
    }

    public void setVedlegg(List<DokumentDto> vedlegg) {
        this.vedlegg = vedlegg;
    }

    public boolean isSkalTilordnes() {
        return skalTilordnes;
    }

    public void setSkalTilordnes(boolean skalTilordnes) {
        this.skalTilordnes = skalTilordnes;
    }

    public boolean isSkalSendesForvaltningsmelding() {
        return skalSendesForvaltningsmelding;
    }

    public void setSkalSendesForvaltningsmelding(boolean skalSendesForvaltningsmelding) {
        this.skalSendesForvaltningsmelding = skalSendesForvaltningsmelding;
    }
    
}
