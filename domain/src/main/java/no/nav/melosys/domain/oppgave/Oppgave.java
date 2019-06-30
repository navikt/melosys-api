package no.nav.melosys.domain.oppgave;

import java.time.LocalDate;
import java.util.Comparator;

import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;

/**
 * Denne klassen mapper Oppgaver fra GSAK og er derfor ikke en @Entity
 */
public class Oppgave {
    private String oppgaveId;
    private String saksnummer;
    private LocalDate fristFerdigstillelse;
    private Tema tema;
    private Oppgavetyper oppgavetype;
    private PrioritetType prioritet;
    private String journalpostId;
    private String tilordnetRessurs;
    private int versjon;
    private String aktørId;
    private Behandlingstyper behandlingstype;
    private Behandlingstema behandlingstema;
    private String behandlesAvApplikasjon;

    public Oppgave() {
    }

    public Behandlingstyper getBehandlingstype() {
        return behandlingstype;
    }

    public void setBehandlingstype(Behandlingstyper behandlingstype) {
        this.behandlingstype = behandlingstype;
    }

    public Behandlingstema getBehandlingstema() {
        return behandlingstema;
    }

    public void setBehandlingstema(Behandlingstema behandlingstema) {
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

    public Oppgavetyper getOppgavetype() {
        return oppgavetype;
    }

    public void setOppgavetype(Oppgavetyper oppgavetype) {
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

    public String getBehandlesAvApplikasjon() {
        return behandlesAvApplikasjon;
    }

    public void setBehandlesAvApplikasjon(String behandlesAvApplikasjon) {
        this.behandlesAvApplikasjon = behandlesAvApplikasjon;
    }

    public boolean erBehandling() {
        return oppgavetype == Oppgavetyper.BEH_SAK_MK;
    }

    public boolean erJournalFøring() {
        return oppgavetype == Oppgavetyper.JFR;
    }

    public boolean erVurderDokument() {
        return oppgavetype == Oppgavetyper.VUR;
    }

    public static Oppgavetyper hentOppgavetype(Behandlingstyper behandlingstype) {
        switch (behandlingstype) {
            case SOEKNAD:
                return Oppgavetyper.BEH_SAK_MK;
            case ENDRET_PERIODE:
                return Oppgavetyper.VUR;
            case ANKE:
            case KLAGE:
            case UTL_MYND_UTPEKT_NORGE:
            case NY_VURDERING:
            case UTL_MYND_UTPEKT_SEG_SELV:
            case REGISTRERING_UNNTAK_NORSK_TRYGD:
            case ANMODNING_OM_UNNTAK_HOVEDREGEL:
            case ØVRIGE_SED:
            default:
                throw new UnsupportedOperationException();
        }
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