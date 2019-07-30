package no.nav.melosys.domain.oppgave;

import java.time.LocalDate;
import java.util.Comparator;

import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;

/**
 * Denne klassen mapper Oppgaver fra GSAK og er derfor ikke en @Entity
 */
public final class Oppgave {
    private final String oppgaveId;
    private final String saksnummer;
    private final LocalDate fristFerdigstillelse;
    private final Tema tema;
    private final Oppgavetyper oppgavetype;
    private final PrioritetType prioritet;
    private final String journalpostId;
    private final String tilordnetRessurs;
    private final int versjon;
    private final String aktørId;
    private final Behandlingstyper behandlingstype;
    private final Behandlingstema behandlingstema;

    public static class Builder {
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

        public Builder setOppgaveId(String oppgaveId) {
            this.oppgaveId = oppgaveId;
            return this;
        }

        public Builder setSaksnummer(String saksnummer) {
            this.saksnummer = saksnummer;
            return this;
        }

        public Builder setFristFerdigstillelse(LocalDate fristFerdigstillelse) {
            this.fristFerdigstillelse = fristFerdigstillelse;
            return this;
        }

        public Builder setTema(Tema tema) {
            this.tema = tema;
            return this;
        }

        public Builder setOppgavetype(Oppgavetyper oppgavetype) {
            this.oppgavetype = oppgavetype;
            return this;
        }

        public Builder setPrioritet(PrioritetType prioritet) {
            this.prioritet = prioritet;
            return this;
        }

        public Builder setJournalpostId(String journalpostId) {
            this.journalpostId = journalpostId;
            return this;
        }

        public Builder setTilordnetRessurs(String tilordnetRessurs) {
            this.tilordnetRessurs = tilordnetRessurs;
            return this;
        }

        public Builder setVersjon(int versjon) {
            this.versjon = versjon;
            return this;
        }

        public Builder setAktørId(String aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder setBehandlingstype(Behandlingstyper behandlingstype) {
            this.behandlingstype = behandlingstype;
            return this;
        }

        public Builder setBehandlingstema(Behandlingstema behandlingstema) {
            this.behandlingstema = behandlingstema;
            return this;
        }

        public Oppgave build() {
            return new Oppgave(this);
        }
    }

    private Oppgave(Builder builder) {
        this.oppgaveId = builder.oppgaveId;
        this.saksnummer = builder.saksnummer;
        this.fristFerdigstillelse = builder.fristFerdigstillelse;
        this.tema = builder.tema;
        this.oppgavetype = builder.oppgavetype;
        this.prioritet = builder.prioritet;
        this.journalpostId = builder.journalpostId;
        this.tilordnetRessurs = builder.tilordnetRessurs;
        this.versjon = builder.versjon;
        this.aktørId = builder.aktørId;
        this.behandlingstype = builder.behandlingstype;
        this.behandlingstema = builder.behandlingstema;
    }

    public String getOppgaveId() {
        return oppgaveId;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public LocalDate getFristFerdigstillelse() {
        return fristFerdigstillelse;
    }

    public Tema getTema() {
        return tema;
    }

    public Oppgavetyper getOppgavetype() {
        return oppgavetype;
    }

    public PrioritetType getPrioritet() {
        return prioritet;
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    public String getTilordnetRessurs() {
        return tilordnetRessurs;
    }

    public int getVersjon() {
        return versjon;
    }

    public String getAktørId() {
        return aktørId;
    }

    public Behandlingstyper getBehandlingstype() {
        return behandlingstype;
    }

    public Behandlingstema getBehandlingstema() {
        return behandlingstema;
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

    public boolean erSed() {
        return oppgavetype == Oppgavetyper.BEH_SED;
    }

    public static Oppgavetyper hentOppgavetype(Behandlingstyper behandlingstype) {
        switch (behandlingstype) {
            case SOEKNAD:
                return Oppgavetyper.BEH_SAK_MK;
            case ENDRET_PERIODE:
                return Oppgavetyper.VUR;
            case UTL_MYND_UTPEKT_SEG_SELV:
            case REGISTRERING_UNNTAK_NORSK_TRYGD:
                return Oppgavetyper.BEH_SED;
            case ANKE:
            case KLAGE:
            case UTL_MYND_UTPEKT_NORGE:
            case NY_VURDERING:
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