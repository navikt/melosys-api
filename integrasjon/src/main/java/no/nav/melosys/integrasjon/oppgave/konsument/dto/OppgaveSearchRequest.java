package no.nav.melosys.integrasjon.oppgave.konsument.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OppgaveSearchRequest {
    private final String tildeltEnhetsnr;
    private final Boolean tildeltRessurs;
    @JsonProperty("aktoerId")
    private final String aktørId;
    private final String[] journalpostId;
    private final String orgnr;
    private final String[] oppgavetype;
    private final String behandlingstype;
    private final String tilordnetRessurs;
    private final String[] tema;
    private final String sorteringsfelt;
    private final String sorteringsrekkefolge;
    private final String statusKategori;
    private final String[] saksreferanse;
    private final String behandlesAvApplikasjon;
    private final String behandlingstema;

    private OppgaveSearchRequest(Builder builder) {
        this.tildeltEnhetsnr = builder.enhetId;
        this.tildeltRessurs = builder.tildeltRessurs;
        this.aktørId = builder.aktørId;
        this.journalpostId = builder.journalpostId;
        this.orgnr = builder.orgnr;
        this.oppgavetype = builder.oppgavetype;
        this.behandlingstype = builder.behandlingstype;
        this.tilordnetRessurs = builder.tilordnetRessurs;
        this.tema = builder.tema;
        this.sorteringsfelt = builder.sorteringsfelt;
        this.sorteringsrekkefolge = builder.sorteringsrekkefolge;
        this.statusKategori = builder.statusKategori;
        this.saksreferanse = builder.saksreferanse;
        this.behandlesAvApplikasjon = builder.behandlesAvApplikasjon;
        this.behandlingstema = builder.behandlingstema;
    }

    public String[] getOppgavetype() {
        return oppgavetype;
    }

    public String getBehandlingstype() {
        return behandlingstype;
    }

    public String[] getTema() {
        return tema;
    }

    public String getSorteringsfelt() {
        return sorteringsfelt;
    }

    public String getSorteringsrekkefolge() {
        return sorteringsrekkefolge;
    }

    public String getTildeltEnhetsnr() {
        return tildeltEnhetsnr;
    }

    public Boolean getTildeltRessurs() {
        return tildeltRessurs;
    }

    public String getAktørId() {
        return aktørId;
    }

    public String[] getJournalpostId() {
        return journalpostId;
    }

    public String getOrgnr() {
        return orgnr;
    }

    public String getTilordnetRessurs() {
        return tilordnetRessurs;
    }

    public String getStatusKategori() {
        return statusKategori;
    }

    public String[] getSaksreferanse() {
        return saksreferanse;
    }

    public String getBehandlesAvApplikasjon() {
        return behandlesAvApplikasjon;
    }

    public String getBehandlingstema() {
        return behandlingstema;
    }

    public static class Builder {

        private final String enhetId;
        private String aktørId;
        private String[] journalpostId;
        private String orgnr;
        private String[] oppgavetype;
        private String behandlingstype;
        private String tilordnetRessurs;
        private Boolean tildeltRessurs;
        private String[] tema;
        private String sorteringsfelt;
        private String sorteringsrekkefolge;
        private String statusKategori;
        private String[] saksreferanse;
        private String behandlesAvApplikasjon;
        private String behandlingstema;

        public Builder(String enhetId) {
            this.enhetId = enhetId;
        }

        public Builder medAktørId(String aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder medJournalpostID(String[] journalpostId) {
            this.journalpostId = journalpostId;
            return this;
        }

        public Builder medOrgnr(String orgnr) {
            this.orgnr = orgnr;
            return this;
        }

        public Builder medBehandlingsType(String behandlingstype) {
            this.behandlingstype = behandlingstype;
            return this;
        }

        public Builder medOppgaveTyper(String[] oppgavetyper) {
            this.oppgavetype = oppgavetyper;
            return this;
        }

        public Builder medSorteringsfelt(String sorteringsfelt) {
            this.sorteringsfelt = sorteringsfelt;
            return this;
        }

        public Builder medSorteringsrekkefolge(String sorteringsrekkefolge) {
            this.sorteringsrekkefolge = sorteringsrekkefolge;
            return this;
        }

        public Builder medTema(String[] tema) {
            this.tema = tema;
            return this;
        }

        public Builder medTildeltRessurs(Boolean tildeltRessurs) {
            this.tildeltRessurs = tildeltRessurs;
            return this;
        }

        public Builder medTilordnetRessurs(String tilordnetRessurs) {
            this.tilordnetRessurs = tilordnetRessurs;
            return this;
        }

        public Builder medStatusKategori(String statusKategori) {
            this.statusKategori = statusKategori;
            return this;
        }

        public Builder medSaksreferanse(String[] saksreferanse) {
            this.saksreferanse = saksreferanse;
            return this;
        }

        public Builder medBehandlesAvApplikasjon(String behandlesAvApplikasjon) {
            this.behandlesAvApplikasjon = behandlesAvApplikasjon;
            return this;
        }

        public Builder medBehandlingstema(String behandlingstema) {
            this.behandlingstema = behandlingstema;
            return this;
        }

        public OppgaveSearchRequest build() {
            return new OppgaveSearchRequest(this);
        }
    }
}
