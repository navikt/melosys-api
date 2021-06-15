package no.nav.melosys.integrasjon.oppgave;

import java.time.LocalDate;

import no.nav.melosys.domain.Fagsystem;
import no.nav.melosys.domain.Tema;
import no.nav.melosys.domain.kodeverk.Oppgavetyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;

public class OppgaveOppdatering {
    private final Oppgavetyper oppgavetype;
    private final Tema tema;
    private final String behandlingstema;
    private final Fagsystem behandlesAvApplikasjon;
    private final String saksnummer;
    private final String beskrivelse;
    private final String prioritet;
    private final String tilordnetRessurs;
    private final String status;
    private final LocalDate fristFerdigstillelse;

    private OppgaveOppdatering(OppgaveOppdateringBuilder builder) {
        this.oppgavetype = builder.oppgavetype;
        this.tema = builder.tema;
        this.behandlingstema = builder.behandlingstema;
        this.behandlesAvApplikasjon = builder.behandlesAvApplikasjon;
        this.saksnummer = builder.saksnummer;
        this.beskrivelse = builder.beskrivelse;
        this.prioritet = builder.prioritet;
        this.tilordnetRessurs = builder.tilordnetRessurs;
        this.status = builder.status;
        this.fristFerdigstillelse = builder.fristFerdigstillelse;
    }

    public static OppgaveOppdateringBuilder builder() {
        return new OppgaveOppdateringBuilder();
    }

    public Oppgavetyper getOppgavetype() {
        return oppgavetype;
    }

    public Tema getTema() {
        return tema;
    }

    public String getBehandlingstema() {
        return behandlingstema;
    }

    public Fagsystem getBehandlesAvApplikasjon() {
        return behandlesAvApplikasjon;
    }

    public String getSaksnummer() {
        return saksnummer;
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public String getPrioritet() {
        return prioritet;
    }

    public String getTilordnetRessurs() {
        return tilordnetRessurs;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getFristFerdigstillelse() {
        return fristFerdigstillelse;
    }

    public static class OppgaveOppdateringBuilder {
        private Oppgavetyper oppgavetype;
        private Tema tema;
        private String behandlingstema;
        private Fagsystem behandlesAvApplikasjon;
        private String saksnummer;
        private String beskrivelse;
        private String prioritet;
        private String tilordnetRessurs;
        private String status;
        private LocalDate fristFerdigstillelse;

        OppgaveOppdateringBuilder() {
        }

        public OppgaveOppdateringBuilder oppgavetype(Oppgavetyper oppgavetype) {
            this.oppgavetype = oppgavetype;
            return this;
        }

        public OppgaveOppdateringBuilder tema(Tema tema) {
            this.tema = tema;
            return this;
        }

        public OppgaveOppdateringBuilder behandlingstema(String behandlingstema) {
            this.behandlingstema = behandlingstema;
            return this;
        }

        public OppgaveOppdateringBuilder behandlesAvApplikasjon(Fagsystem fagsystem) {
            this.behandlesAvApplikasjon = fagsystem;
            return this;
        }

        public OppgaveOppdateringBuilder saksnummer(String saksnummer) {
            this.saksnummer = saksnummer;
            return this;
        }

        public OppgaveOppdateringBuilder beskrivelse(String beskrivelse) {
            this.beskrivelse = beskrivelse;
            return this;
        }

        public OppgaveOppdateringBuilder prioritet(String prioritet) {
            this.prioritet = prioritet;
            return this;
        }

        public OppgaveOppdateringBuilder tilordnetRessurs(String tilordnetRessurs) {
            this.tilordnetRessurs = tilordnetRessurs;
            return this;
        }

        public OppgaveOppdateringBuilder status(String status) {
            this.status = status;
            return this;
        }

        public OppgaveOppdateringBuilder fristFerdigstillelse(LocalDate fristFerdigstillelse) {
            this.fristFerdigstillelse = fristFerdigstillelse;
            return this;
        }

        public OppgaveOppdatering build() {
            return new OppgaveOppdatering(this);
        }
    }
}
