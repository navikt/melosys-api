package no.nav.melosys.service.oppgave.dto;


import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.oppgave.PrioritetType;

public class OppgaveDto {
    private String oppgaveID;
    private String oppgavetypeKode;
    private String sammensattNavn;
    private String saksnummer;
    private String journalpostID;
    private LocalDate aktivTil;
    private PeriodeDto soknadsperiode;
    private String sakstypeKode;
    private BehandlingDto behandling;
    private PrioritetType prioritet;
    private List<String> land;
    private int versjon;
    private String ansvarligID;
    // FIXME: MELOSYS-1401 trenger frontend behandlingstype,behandlingstema,temagruppe ?

    public OppgaveDto() {
        this.soknadsperiode = new PeriodeDto();
        this.behandling = new BehandlingDto();
    }

    //Getter og setter brukes av Jackson for å serialisere oppgave objekter til frontend i OppgaveTjeneste
    public String getAnsvarligID() {
        return ansvarligID;
    }

    public void setAnsvarligID(String ansvarligID) {
        this.ansvarligID = ansvarligID;
    }

    public int getVersjon() {
        return versjon;
    }

    public void setVersjon(int versjon) {
        this.versjon = versjon;
    }

    public BehandlingDto getBehandling() {
        return behandling;
    }

    public void setBehandling(BehandlingDto behandling) {
        this.behandling = behandling;
    }

    public List<String> getLand() {
        return this.land;
    }

    public void setLand(List<String> land) {
        this.land = land;
    }

    public String getOppgaveID() {
        return oppgaveID;
    }

    public void setOppgaveID(String oppgaveID) {
        this.oppgaveID = oppgaveID;
    }

    public String getOppgavetypeKode() {
        return oppgavetypeKode;
    }

    public void setOppgavetypeKode(String oppgavetypeKode) {
        this.oppgavetypeKode = oppgavetypeKode;
    }

    public String getSakstypeKode() {
        return sakstypeKode;
    }

    public void setSakstypeKode(String sakstypeKode) {
        this.sakstypeKode = sakstypeKode;
    }

    public String getSammensattNavn() {
        return sammensattNavn;
    }

    public void setSammensattNavn(String sammensattNavn) {
        this.sammensattNavn = sammensattNavn;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public String getJournalpostID() {
        return journalpostID;
    }

    public void setJournalpostID(String journalpostID) {
        this.journalpostID = journalpostID;
    }

    public LocalDate getAktivTil() {
        return aktivTil;
    }

    public void setAktivTil(LocalDate aktivTil) {
        this.aktivTil = aktivTil;
    }

    public PeriodeDto getSoknadsperiode() {
        return soknadsperiode;
    }

    public void setSoknadsperiode(PeriodeDto soknadsperiode) {
        this.soknadsperiode = soknadsperiode;
    }

    public PrioritetType getPrioritet() {
        return prioritet;
    }

    public void setPrioritet(PrioritetType prioritet) {
        this.prioritet = prioritet;
    }
}
