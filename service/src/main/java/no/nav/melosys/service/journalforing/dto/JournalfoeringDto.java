package no.nav.melosys.service.journalforing.dto;

import java.util.List;

import no.nav.melosys.domain.arkiv.AvsenderType;

public class JournalfoeringDto {
    private String behandlingstypeKode;
    private String journalpostID;
    private String oppgaveID;
    private String brukerID;
    private String avsenderID;
    private String avsenderNavn;
    private AvsenderType avsenderType;
    private String dokumentID;
    private String hoveddokumentTittel;
    private List<DokumentDto> vedlegg;
    private boolean skalTilordnes;
    private Boolean ikkeSendForvaltingsmelding;

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

    public AvsenderType getAvsenderType() {
        return avsenderType;
    }

    public void setAvsenderType(AvsenderType avsenderType) {
        this.avsenderType = avsenderType;
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

    public Boolean isIkkeSendForvaltingsmelding() {
        return ikkeSendForvaltingsmelding;
    }

    public void setIkkeSendForvaltingsmelding(Boolean ikkeSendForvaltingsmelding) {
        this.ikkeSendForvaltingsmelding = ikkeSendForvaltingsmelding;
    }
    
}
