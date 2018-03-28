package no.nav.melosys.integrasjon.gsak.oppgave;


import java.util.List;

public class FinnOppgaveListeSokMal {
    private String ansvarligEnhetId;
    private String ansvarligId;
    private String brukerId;
    private String sakId;
    private List<String> fagområdeKodeListe;


    public FinnOppgaveListeSokMal(String ansvarligEnhetId, String ansvarligId, String sakId, List<String> fagområdeKodeListe) {
        this.ansvarligEnhetId = ansvarligEnhetId;
        this.ansvarligId=ansvarligId;
        this.brukerId = brukerId;
        this.fagområdeKodeListe = fagområdeKodeListe;;
        this.sakId = sakId;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getAnsvarligEnhetId() {
        return ansvarligEnhetId;
    }

    public String getBrukerId() {
        return brukerId;
    }

    public List<String> getFagområdeKodeListe() {
        return fagområdeKodeListe;
    }

    public String getSakId() {
        return sakId;
    }

    public static class Builder {
        private String ansvarligEnhetId;
        private String ansvarligId;
        private String brukerId;
        private String sakId;
        private List<String> fagområdeKodeListe;

        public Builder medAnsvarligEnhetId(String ansvarligEnhetId) {
            this.ansvarligEnhetId = ansvarligEnhetId;
            return this;
        }

        public Builder medBrukerId(String brukerId) {
            this.brukerId = brukerId;
            return this;
        }

        public Builder medSakId(String sakId) {
            this.sakId = sakId;
            return this;
        }

        public Builder medFagområdeKodeListe(List<String> fagområdeKodeListe) {
            this.fagområdeKodeListe = fagområdeKodeListe;
            return this;
        }

        public FinnOppgaveListeSokMal build() {
            return new FinnOppgaveListeSokMal(ansvarligEnhetId, ansvarligId, sakId, fagområdeKodeListe);
        }

        public Builder medAnsvarligId(String ansvarligId) {
            this.ansvarligId = ansvarligId;
            return this;
        }
    }
}

