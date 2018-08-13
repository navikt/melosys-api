package no.nav.melosys.service.oppgave.dto;


import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.domain.oppgave.Oppgavetype;
import no.nav.melosys.domain.oppgave.PrioritetType;

public class OppgaveDto {
    private String oppgaveID;
    private Oppgavetype oppgavetype;
    private String sammensattNavn;
    private String saksnummer;
    private String journalpostID;
    private LocalDate aktivTil;
    private PeriodeDto soknadsperiode;
    private FagsakType sakstype;
    private BehandlingDto behandling;
    private PrioritetType prioritet;
    private List<String> land;
    private int versjon;
    private String ansvarligId;
    // FIXME: MELOSYS-1401 trenger frontend behandlingstype,behandlingstema,temagruppe ?

    //Getter og setter brukes av Jackson for å serialisere oppgave objekter til frontend i OppgaveTjeneste
    public String getAnsvarligId() {
        return ansvarligId;
    }

    public void setAnsvarligId(String ansvarligId) {
        this.ansvarligId = ansvarligId;
    }

    public int getVersjon() {
        return versjon;
    }

    public void setVersjon(int versjon) {
        this.versjon = versjon;
    }

    public FagsakType getSakstype() {
        return sakstype;
    }

    public void setSakstype(FagsakType sakstype) {
        this.sakstype = sakstype;
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

    public Oppgavetype getOppgavetype() {
        return oppgavetype;
    }

    public void setOppgavetype(Oppgavetype oppgavetype) {
        this.oppgavetype = oppgavetype;
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
