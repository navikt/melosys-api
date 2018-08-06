package no.nav.melosys.domain.oppgave;

import java.time.LocalDate;
import java.util.Comparator;

import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.Tema;

/**
 * Denne klassen mapper Oppgaver fra GSAK og er derfor ikke en @Entity
 */
public class Oppgave {
    private String oppgaveId;
    private String saksnummer;
    private LocalDate fristFerdigstillelse;
    private Tema tema;
    private Oppgavetype oppgavetype;
    private PrioritetType prioritet;
    private String gsakSaksnummer;
    private String journalpostId;
    private String tilordnetRessurs;
    private int versjon;
    private String aktørId;
    // FIXME: MELOSYS-1401 : skal implementere logikk rundt disse
    private BehandlingType behandlingstype;
    private Behandlingtema behandlingstema;
    private Temagruppe temagruppe;

    public Oppgave() {
    }

    public BehandlingType getBehandlingstype() {
        return behandlingstype;
    }

    public void setBehandlingstype(BehandlingType behandlingstype) {
        this.behandlingstype = behandlingstype;
    }

    public Behandlingtema getBehandlingstema() {
        return behandlingstema;
    }

    public void setBehandlingstema(Behandlingtema behandlingstema) {
        this.behandlingstema = behandlingstema;
    }

    public Temagruppe getTemagruppe() {
        return temagruppe;
    }

    public void setTemagruppe(Temagruppe temagruppe) {
        this.temagruppe = temagruppe;
    }

    public String getAktørId() {
        return aktørId;
    }

    public void setAktørId(String aktørId) {
        this.aktørId = aktørId;
    }

    public String getTilordnetRessurs() {
        return tilordnetRessurs;
    }

    public void setTilordnetRessurs(String tilordnetRessurs) {
        this.tilordnetRessurs = tilordnetRessurs;
    }

    public boolean erBehandling() {
        return oppgavetype == Oppgavetype.BEH_SAK;
    }

    public boolean erJournalFøring() {
        return oppgavetype == Oppgavetype.JFR;
    }

    public String getOppgaveId() {
        return oppgaveId;
    }

    public void setOppgaveId(String oppgaveId) {
        this.oppgaveId = oppgaveId;
    }

    public LocalDate getFristFerdigstillelse() {
        return fristFerdigstillelse;
    }

    public void setFristFerdigstillelse(LocalDate fristFerdigstillelse) {
        this.fristFerdigstillelse = fristFerdigstillelse;
    }

    public Oppgavetype getOppgavetype() {
        return oppgavetype;
    }

    public void setOppgavetype(Oppgavetype oppgavetype) {
        this.oppgavetype = oppgavetype;
    }

    public PrioritetType getPrioritet() {
        return prioritet;
    }

    public void setPrioritet(PrioritetType prioritet) {
        this.prioritet = prioritet;
    }

    public String getGsakSaksnummer() {
        return gsakSaksnummer;
    }

    public void setGsakSaksnummer(String gsakSaksnummer) {
        this.gsakSaksnummer = gsakSaksnummer;
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    public void setJournalpostId(String journalpostId) {
        this.journalpostId = journalpostId;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public void setSaksnummer(String saksnummer) {
        this.saksnummer = saksnummer;
    }

    public Tema getTema() {
        return tema;
    }

    public void setTema(Tema tema) {
        this.tema = tema;
    }

    public int getVersjon() {
        return versjon;
    }

    public void setVersjon(int versjon) {
        this.versjon = versjon;
    }

}