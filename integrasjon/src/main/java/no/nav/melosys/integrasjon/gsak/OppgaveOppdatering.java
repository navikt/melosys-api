package no.nav.melosys.integrasjon.gsak;

import java.time.LocalDate;

public class OppgaveOppdatering {

    private String beskrivelse;
    private String prioritet;
    private String tilordnetRessurs;
    private String status;
    private LocalDate fristFerdigstillelse;

    OppgaveOppdatering(String beskrivelse, String prioritet, String tilordnetRessurs, String status, LocalDate fristFerdigstillelse) {
        this.beskrivelse = beskrivelse;
        this.prioritet = prioritet;
        this.tilordnetRessurs = tilordnetRessurs;
        this.status = status;
        this.fristFerdigstillelse = fristFerdigstillelse;
    }

    public static OppgaveOppdateringBuilder builder() {
        return new OppgaveOppdateringBuilder();
    }

    public String getBeskrivelse() {
        return beskrivelse;
    }

    public void setBeskrivelse(String beskrivelse) {
        this.beskrivelse = beskrivelse;
    }

    public String getPrioritet() {
        return prioritet;
    }

    public void setPrioritet(String prioritet) {
        this.prioritet = prioritet;
    }

    public String getTilordnetRessurs() {
        return tilordnetRessurs;
    }

    public void setTilordnetRessurs(String tilordnetRessurs) {
        this.tilordnetRessurs = tilordnetRessurs;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getFristFerdigstillelse() {
        return fristFerdigstillelse;
    }

    public void setFristFerdigstillelse(LocalDate fristFerdigstillelse) {
        this.fristFerdigstillelse = fristFerdigstillelse;
    }

    public static class OppgaveOppdateringBuilder {
        private String beskrivelse;
        private String prioritet;
        private String tilordnetRessurs;
        private String status;
        private LocalDate fristFerdigstillelse;

        OppgaveOppdateringBuilder() {
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
            return new OppgaveOppdatering(beskrivelse, prioritet, tilordnetRessurs, status, fristFerdigstillelse);
        }
    }
}
