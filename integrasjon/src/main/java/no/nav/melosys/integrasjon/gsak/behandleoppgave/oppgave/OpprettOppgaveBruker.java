package no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave;

import no.nav.melosys.integrasjon.gsak.behandleoppgave.oppgave.kodeverk.AktorType;

public class OpprettOppgaveBruker {
    private String fnr;
    private AktorType aktørType;

    private OpprettOppgaveBruker(String fnr, AktorType aktørType) {
        this.fnr = fnr;
        this.aktørType = aktørType;
    }

    public String getFnr() {
        return fnr;
    }

    public AktorType getAktørType() {
        return aktørType;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String fnr;
        private AktorType aktørType;

        public Builder medFødselsnummer(String fnr) {
            this.fnr = fnr;
            return this;
        }

        public Builder medAktørType(AktorType aktørType) {
            this.aktørType = aktørType;
            return this;
        }

        public OpprettOppgaveBruker build() {
            return new OpprettOppgaveBruker(fnr, aktørType);
        }
    }
}
