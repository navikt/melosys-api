package no.nav.melosys.integrasjon.gsak.oppgave.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OppgaveSearchRequest {
    private String tildeltEnhetsnr;
    private Boolean tildeltRessurs;
    @JsonProperty("aktoerId")
    private String aktørId;
    private String[] oppgavetype;
    private String behandlingstype;
    private String tilordnetRessurs;
    private String[] tema;
    private String sorteringsfelt;
    private String statusKategori;
    private String[] saksreferanse;
    private String behandlesAvApplikasjon;
    private String behandlingstema;

    private OppgaveSearchRequest(Builder builder) {
        this.tildeltEnhetsnr = builder.enhetId;
        this.tildeltRessurs = builder.tildeltRessurs;
        this.aktørId = builder.aktørId;
        this.oppgavetype = builder.oppgavetype;
        this.behandlingstype = builder.behandlingstype;
        this.tilordnetRessurs = builder.tilordnetRessurs;
        this.tema = builder.tema;
        this.sorteringsfelt = builder.sorteringsfelt;
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

    public String getTildeltEnhetsnr() {
        return tildeltEnhetsnr;
    }

    public Boolean getTildeltRessurs() {
        return tildeltRessurs;
    }

    public String getAktørId() {
        return aktørId;
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

        private String enhetId;
        private String aktørId;
        private String[] oppgavetype;
        private String behandlingstype;
        private String tilordnetRessurs;
        private Boolean tildeltRessurs;
        private String[] tema;
        private String sorteringsfelt;
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
