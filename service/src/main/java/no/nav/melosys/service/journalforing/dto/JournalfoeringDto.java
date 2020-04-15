package no.nav.melosys.service.journalforing.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.kodeverk.Avsendertyper;

public abstract class JournalfoeringDto {
    private String journalpostID;
    private String oppgaveID;
    private String brukerID;
    private String avsenderID;
    private String avsenderNavn;
    private Avsendertyper avsenderType;
    private DokumentDto hoveddokument;
    private List<DokumentDto> vedlegg = new ArrayList<>();
    private boolean skalTilordnes;
    private Boolean ikkeSendForvaltingsmelding;
    private LocalDate mottattDato;

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

    public Avsendertyper getAvsenderType() {
        return avsenderType;
    }

    public void setAvsenderType(Avsendertyper avsenderType) {
        this.avsenderType = avsenderType;
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

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public void setMottattDato(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
    }
}