package no.nav.melosys.service.journalforing.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.kodeverk.Avsendertyper;
import no.nav.melosys.domain.kodeverk.ForvaltningsmeldingMottaker;

import static no.nav.melosys.domain.kodeverk.ForvaltningsmeldingMottaker.AVSENDER;
import static no.nav.melosys.domain.kodeverk.ForvaltningsmeldingMottaker.BRUKER;

public abstract class JournalfoeringDto {
    protected String journalpostID;
    protected String oppgaveID;
    protected String brukerID;
    protected String virksomhetOrgnr;
    protected String avsenderID;
    protected String avsenderNavn;
    protected Avsendertyper avsenderType;
    protected DokumentDto hoveddokument;
    protected List<DokumentDto> vedlegg = new ArrayList<>();
    protected boolean skalTilordnes;
    protected LocalDate mottattDato;
    protected String behandlingstemaKode;
    protected String behandlingstypeKode;
    protected ForvaltningsmeldingMottaker forvaltningsmeldingMottaker;

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

    public String getVirksomhetOrgnr() {
        return virksomhetOrgnr;
    }

    public void setVirksomhetOrgnr(String virksomhetOrgnr) {
        this.virksomhetOrgnr = virksomhetOrgnr;
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

    public LocalDate getMottattDato() {
        return mottattDato;
    }

    public void setMottattDato(LocalDate mottattDato) {
        this.mottattDato = mottattDato;
    }

    public String getBehandlingstemaKode() {
        return behandlingstemaKode;
    }

    public void setBehandlingstemaKode(String behandlingstemaKode) {
        this.behandlingstemaKode = behandlingstemaKode;
    }

    public String getBehandlingstypeKode() {
        return behandlingstypeKode;
    }

    public void setBehandlingstypeKode(String behandlingstypeKode) {
        this.behandlingstypeKode = behandlingstypeKode;
    }

    public boolean skalSendeForvaltningsmelding() {
        return BRUKER.equals(forvaltningsmeldingMottaker) || AVSENDER.equals(forvaltningsmeldingMottaker);
    }

    public ForvaltningsmeldingMottaker getForvaltningsmeldingMottaker() {
        return forvaltningsmeldingMottaker;
    }

    public void setForvaltningsmeldingMottaker(ForvaltningsmeldingMottaker forvaltningsmeldingMottaker) {
        this.forvaltningsmeldingMottaker = forvaltningsmeldingMottaker;
    }
}
