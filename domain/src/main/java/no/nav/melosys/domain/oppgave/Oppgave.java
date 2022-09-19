package no.nav.melosys.domain.oppgave;

import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.Objects;

import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;

public final class Oppgave {
    private final String aktørId;
    private final String orgnr;
    private final String behandlingstema;
    private final String behandlingstype;
    private final String beskrivelse;
    private final Fagsystem behandlesAvApplikasjon;
    private final ZonedDateTime opprettetTidspunkt;
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
        private String orgnr;
        private Fagsystem behandlesAvApplikasjon;
        private String behandlingstema;
        private String behandlingstype;
        private String beskrivelse;
        private ZonedDateTime opprettetTidspunkt;
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

        public Builder setOpprettetTidspunkt(ZonedDateTime opprettetTidspunkt) {
            this.opprettetTidspunkt = opprettetTidspunkt;
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

        public Builder setOrgnr(String orgnr) {
            this.orgnr = orgnr;
            return this;
        }


        public Builder setBehandlingstype(String behandlingstype) {
            this.behandlingstype = behandlingstype;
            return this;
        }

        public Builder setBehandlingstema(String behandlingstema) {
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
        this.opprettetTidspunkt = builder.opprettetTidspunkt;
        this.fristFerdigstillelse = builder.fristFerdigstillelse;
        this.tema = builder.tema;
        this.oppgavetype = builder.oppgavetype;
        this.prioritet = builder.prioritet != null ? builder.prioritet : PrioritetType.NORM;
        this.journalpostId = builder.journalpostId;
        this.tilordnetRessurs = builder.tilordnetRessurs;
        this.versjon = builder.versjon;
        this.aktørId = builder.aktørId;
        this.orgnr = builder.orgnr;
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

    public String getOrgnr() {
        return orgnr;
    }

    public Fagsystem getBehandlesAvApplikasjon() {
        return behandlesAvApplikasjon;
    }

    public String getBehandlingstema() {
        return behandlingstema;
    }

    public String getBehandlingstype() {
        return behandlingstype;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public ZonedDateTime getOpprettetTidspunkt() {
        return opprettetTidspunkt;
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

    public boolean erVurderHenvendelse() {
        return oppgavetype == Oppgavetyper.VURD_HENV;
    }

    public boolean erSedBehandling() {
        return oppgavetype == Oppgavetyper.BEH_SED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Oppgave oppgave = (Oppgave) o;
        return Objects.equals(oppgaveId, oppgave.oppgaveId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oppgaveId);
    }

    /**
     *  Sorter oppgaver basert på prioritet (først), frist og aktiv dato
     */
    public static final Comparator<Oppgave> lavestTilHøyestPrioritet = (a, b) -> {
        int res;

        res = a.getPrioritet().compareTo(b.getPrioritet());

        if (res == 0) {
            res = Math.negateExact(compareNullableLocaldate(a.getFristFerdigstillelse(), b.getFristFerdigstillelse()));
            if (res == 0) {
                res = Math.negateExact(compareNullableLocaldate(a.getAktivDato(), b.getAktivDato()));
            }
        }

        return res;
    };

    private static int compareNullableLocaldate(LocalDate a, LocalDate b) {
        if (a != null && b != null) return a.compareTo(b);
        else if (a == null && b == null) return 0;
        else if (a == null) return -1;
        else return 1;
    }
}
