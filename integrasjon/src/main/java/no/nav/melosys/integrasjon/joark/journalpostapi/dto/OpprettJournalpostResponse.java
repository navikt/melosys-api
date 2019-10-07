package no.nav.melosys.integrasjon.joark.journalpostapi.dto;

import java.util.List;

public class OpprettJournalpostResponse {
    private String journalpostId;
    private List<Dokument> dokumenter;
    private String journalstatus;
    private String melding;

    public OpprettJournalpostResponse(String journalpostId, List<Dokument> dokumenter, String journalstatus, String melding) {
        this.journalpostId = journalpostId;
        this.dokumenter = dokumenter;
        this.journalstatus = journalstatus;
        this.melding = melding;
    }

    public OpprettJournalpostResponse() {
    }

    public static OpprettJournalpostResponseBuilder builder() {
        return new OpprettJournalpostResponseBuilder();
    }

    public String getJournalpostId() {
        return this.journalpostId;
    }

    public List<Dokument> getDokumenter() {
        return this.dokumenter;
    }

    public String getJournalstatus() {
        return this.journalstatus;
    }

    public String getMelding() {
        return this.melding;
    }

    public static class Dokument {
        private String dokumentInfoId;

        public Dokument(String dokumentInfoId) {
            this.dokumentInfoId = dokumentInfoId;
        }

        public Dokument() {
        }

        public String getDokumentInfoId() {
            return this.dokumentInfoId;
        }

        public void setDokumentInfoId(String dokumentInfoId) {
            this.dokumentInfoId = dokumentInfoId;
        }
    }

    public boolean erFerdigstilt() {
        return JournalfoeringStatus.ENDELIG.name().equals(journalstatus);
    }

    public static class OpprettJournalpostResponseBuilder {
        private String journalpostId;
        private List<Dokument> dokumenter;
        private String journalstatus;
        private String melding;

        OpprettJournalpostResponseBuilder() {
        }

        public OpprettJournalpostResponse.OpprettJournalpostResponseBuilder journalpostId(String journalpostId) {
            this.journalpostId = journalpostId;
            return this;
        }

        public OpprettJournalpostResponse.OpprettJournalpostResponseBuilder dokumenter(List<Dokument> dokumenter) {
            this.dokumenter = dokumenter;
            return this;
        }

        public OpprettJournalpostResponse.OpprettJournalpostResponseBuilder journalstatus(String journalstatus) {
            this.journalstatus = journalstatus;
            return this;
        }

        public OpprettJournalpostResponse.OpprettJournalpostResponseBuilder melding(String melding) {
            this.melding = melding;
            return this;
        }

        public OpprettJournalpostResponse build() {
            return new OpprettJournalpostResponse(journalpostId, dokumenter, journalstatus, melding);
        }
    }
}
