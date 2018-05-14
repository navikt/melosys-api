package no.nav.melosys.integrasjon.gsak.oppgave.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OppgaveSearchRequest {
    private String enhetId;
    @JsonProperty("aktoerId")
    private String aktørId;
    private String[] oppgavetype;
    private String[] behandlingstype;
    private String tilordnetRessurs;
    private String[] tema;
    private String sorteringsfelt;

    private OppgaveSearchRequest(Builder builder) {
        this.enhetId = builder.enhetId;
        this.aktørId = builder.aktørId;
        this.oppgavetype = builder.oppgavetype;
        this.behandlingstype = builder.behandlingstype;
        this.tilordnetRessurs = builder.tilordnetRessurs;
        this.tema = builder.tema;
        this.sorteringsfelt = builder.sorteringsfelt;
    }

    public String[] getOppgavetype() {
        return oppgavetype.clone();
    }

    public String[] getBehandlingstype() {
        return behandlingstype.clone();
    }

    public String[] getTema() {
        return tema.clone();
    }

    public String getSorteringsfelt() {
        return sorteringsfelt;
    }

    public String getEnhetId() {
        return enhetId;
    }

    public String getAktørId() {
        return aktørId;
    }

    public String getTilordnetRessurs() {
        return tilordnetRessurs;
    }

    public static class Builder {

        private String enhetId;
        private String aktørId;
        private String[] oppgavetype;
        private String[] behandlingstype;
        private String tilordnetRessurs;
        private String[] tema;
        private String sorteringsfelt;

        public Builder(String enhetId) {
            this.enhetId = enhetId;
        }

        public Builder medOppgaveTyper(String[] oppgavetyper) {
            this.oppgavetype = oppgavetyper.clone();
            return this;
        }

        public Builder medBehandlingsTyper(String[] behandlingstyper) {
            this.behandlingstype = behandlingstyper.clone();
            return this;
        }

        public Builder medSorteringsfelt(String sorteringsfelt) {
            this.sorteringsfelt = sorteringsfelt;
            return this;
        }

        public Builder tilordnetRessurs(String tilordnetRessurs) {
            this.tilordnetRessurs = tilordnetRessurs;
            return this;
        }

        public Builder aktørId(String aktørId) {
            this.aktørId = aktørId;
            return this;
        }

        public Builder medTema(String[] tema) {
            this.tema = tema.clone();
            return this;
        }

        public OppgaveSearchRequest build() {
            return new OppgaveSearchRequest(this);
        }
    }
}
