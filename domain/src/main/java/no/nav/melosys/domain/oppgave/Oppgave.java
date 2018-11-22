package no.nav.melosys.domain.oppgave;

import java.time.LocalDate;
import java.util.Comparator;

import no.nav.melosys.domain.Behandlingstype;
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
    private String journalpostId;
    private String tilordnetRessurs;
    private int versjon;
    private String aktørId;
    // FIXME: MELOSYS-1401 : skal implementere logikk rundt disse
    private Behandlingstype behandlingstype;
    private BehandlingTema behandlingstema;

    public Oppgave() {
    }

    public Behandlingstype getBehandlingstype() {
        return behandlingstype;
    }

    public void setBehandlingstype(Behandlingstype behandlingstype) {
        this.behandlingstype = behandlingstype;
    }

    public BehandlingTema getBehandlingstema() {
        return behandlingstema;
    }

    public void setBehandlingstema(BehandlingTema behandlingstema) {
        this.behandlingstema = behandlingstema;
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

    /**
     * Sorter oppgaver basert på prioritet (først) og frist.
     */
    public static final Comparator<Oppgave> høyestTilLavestPrioritet = (a, b) -> {
        // Merk: Bryter med konvensjonen (a == b og b == c → a == c), men dette er ok.
        int res = 0;
        if (a.getPrioritet() == b.getPrioritet())
            res = 0;
        else if (a.getPrioritet() == PrioritetType.HOY)
            res = -1;
        else if (b.getPrioritet() == PrioritetType.HOY)
            res = 1;
        else if (a.getPrioritet() == PrioritetType.NORM)
            res = -1;
        else if (b.getPrioritet() == PrioritetType.NORM)
            res = 1;
        if (res == 0) {
            if (a.getFristFerdigstillelse() == null || b.getFristFerdigstillelse() == null)
                return 0;
            return a.getFristFerdigstillelse().compareTo(b.getFristFerdigstillelse());
        }
        return res;
    };

}