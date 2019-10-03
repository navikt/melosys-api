package no.nav.melosys.domain.oppgave;

import java.time.LocalDate;
import java.util.Comparator;

import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;

/**
 * Denne klassen mapper Oppgaver fra GSAK og er derfor ikke en @Entity
 */
public final class Oppgave {
    private static final int FRIST_JFR_DAGER = 1;
    private static final int FRIST_VUR_DAGER = 1;
    private static final int FRIST_BEH_UKER = 12;

    private final String aktørId;
    private final Behandlingstema behandlingstema;
    private final Behandlingstyper behandlingstype;
    private final String beskrivelse;
    private final Fagsystem behandlesAvApplikasjon;
    private final LocalDate fristFerdigstillelse;
    private final String journalpostId;
    private final String oppgaveId;
    private final Oppgavetyper oppgavetype;
    private final PrioritetType prioritet;
    private final String saksnummer;
    private final Tema tema;
    private final String temagruppe;
    private final String tilordnetRessurs;
    private final String tildeltEnhetsnr;
    private final int versjon;
    private final LocalDate aktivDato;
    private final String status;

    public static class Builder {
        private String aktørId;
        private Fagsystem behandlesAvApplikasjon;
        private Behandlingstema behandlingstema;
        private Behandlingstyper behandlingstype;
        private String beskrivelse;
        private LocalDate fristFerdigstillelse;
        private String journalpostId;
        private String oppgaveId;
        private Oppgavetyper oppgavetype;
        private PrioritetType prioritet;
        private String saksnummer;
        private Tema tema;
        private String temagruppe;
        private String tilordnetRessurs;
        private String tildeltEnhetsnr;
        private int versjon;
        private LocalDate aktivDato;
        private String status;

        public Builder() {
        }
        
        public Builder(Oppgave copy) {
            this.aktørId = copy.getAktørId();
            this.behandlingstema = copy.getBehandlingstema();
            this.behandlingstype = copy.getBehandlingstype();
            this.beskrivelse = copy.getBeskrivelse();
            this.behandlesAvApplikasjon = copy.getBehandlesAvApplikasjon();
            this.fristFerdigstillelse = copy.getFristFerdigstillelse();
            this.journalpostId = copy.getJournalpostId();
            this.oppgaveId = copy.getOppgaveId();
            this.oppgavetype = copy.getOppgavetype();
            this.prioritet = copy.getPrioritet();
            this.saksnummer = copy.getSaksnummer();
            this.tema = copy.getTema();
            this.temagruppe = copy.getTemagruppe();
            this.tilordnetRessurs = copy.getTilordnetRessurs();
            this.tildeltEnhetsnr = copy.getTildeltEnhetsnr();
            this.versjon = copy.getVersjon();
            this.aktivDato = copy.getAktivDato();
            this.status = copy.getStatus();
        }
        
        public Builder setOppgaveId(String oppgaveId) {
            this.oppgaveId = oppgaveId;
            return this;
        }

        public Builder setBehandlesAvApplikasjon(Fagsystem fagsystem) {
            this.behandlesAvApplikasjon = fagsystem;
            return this;
        }

        public Builder setSaksnummer(String saksnummer) {
            this.saksnummer = saksnummer;
            return this;
        }

        public Builder setBeskrivelse(String beskrivelse) {
            this.beskrivelse = beskrivelse;
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

        public Builder setTemagruppe(String temagruppe) {
            this.temagruppe = temagruppe;
            return this;
        }

        public Builder setTildeltEnhetsnr(String tildeltEnhetsnr) {
            this.tildeltEnhetsnr = tildeltEnhetsnr;
            return this;
        }

        public Builder setAktivDato(LocalDate aktivDato) {
            this.aktivDato = aktivDato;
            return this;
        }

        public Builder setStatus(String status) {
            this.status = status;
            return this;
        }

        public Oppgave build() {
            return new Oppgave(this);
        }
    }

    private Oppgave(Builder builder) {
        this.oppgaveId = builder.oppgaveId;
        this.behandlesAvApplikasjon = builder.behandlesAvApplikasjon != null ? builder.behandlesAvApplikasjon : Fagsystem.MELOSYS;
        this.saksnummer = builder.saksnummer;
        this.fristFerdigstillelse = builder.fristFerdigstillelse;
        this.tema = builder.tema;
        this.oppgavetype = builder.oppgavetype;
        this.prioritet = builder.prioritet != null ? builder.prioritet : PrioritetType.NORM;
        this.journalpostId = builder.journalpostId;
        this.tilordnetRessurs = builder.tilordnetRessurs;
        this.versjon = builder.versjon;
        this.aktørId = builder.aktørId;
        this.behandlingstype = builder.behandlingstype;
        this.behandlingstema = builder.behandlingstema;
        this.beskrivelse = builder.beskrivelse;
        this.tildeltEnhetsnr = builder.tildeltEnhetsnr;
        this.status = builder.status;
        this.aktivDato = builder.aktivDato;
        this.temagruppe = builder.temagruppe;
    }

    public String getAktørId() {
        return aktørId;
    }

    public Fagsystem getBehandlesAvApplikasjon() {
        return behandlesAvApplikasjon;
    }

    public Behandlingstema getBehandlingstema() {
        return behandlingstema;
    }

    public Behandlingstyper getBehandlingstype() {
        return behandlingstype;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public LocalDate getFristFerdigstillelse() {
        return fristFerdigstillelse;
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    public String getOppgaveId() {
        return oppgaveId;
    }

    public Oppgavetyper getOppgavetype() {
        return oppgavetype;
    }

    public PrioritetType getPrioritet() {
        return prioritet;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public Tema getTema() {
        return tema;
    }

    public String getTilordnetRessurs() {
        return tilordnetRessurs;
    }

    public int getVersjon() {
        return versjon;
    }

    public String getTemagruppe() {
        return temagruppe;
    }

    public String getTildeltEnhetsnr() {
        return tildeltEnhetsnr;
    }

    public LocalDate getAktivDato() {
        return aktivDato;
    }

    public String getStatus() {
        return status;
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

    public boolean erSedBehandling() {
        return oppgavetype == Oppgavetyper.BEH_SED;
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

    public LocalDate lagFristFerdigstillelse(LocalDate fraDato) throws FunksjonellException {
        if (erJournalFøring()) {
            return fraDato.plusDays(FRIST_JFR_DAGER);
        } else if (erBehandling() || erSedBehandling()) {
            return fraDato.plusWeeks(FRIST_BEH_UKER);
        } else if (erVurderDokument()) {
            return fraDato.plusWeeks(FRIST_VUR_DAGER);
        } else {
            throw new FunksjonellException("Type " + oppgavetype.getKode() + " støttes ikke.");
        }
    }
}