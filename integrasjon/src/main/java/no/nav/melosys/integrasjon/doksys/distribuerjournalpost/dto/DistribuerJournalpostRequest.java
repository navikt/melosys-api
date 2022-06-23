package no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto;


import no.nav.melosys.domain.arkiv.Distribusjonstidspunkt;
import no.nav.melosys.domain.arkiv.Distribusjonstype;

public final class DistribuerJournalpostRequest {

    private String journalpostId;
    private String batchId;
    private String bestillendeFagsystem;
    private String dokumentProdApp;
    private Adresse adresse;
    private Distribusjonstype distribusjonstype;
    private Distribusjonstidspunkt distribusjonstidspunkt;

    public DistribuerJournalpostRequest(String journalpostId, String batchId,
                                        String bestillendeFagsystem,
                                        String dokumentProdApp,
                                        Adresse adresse,
                                        Distribusjonstype distribusjonstype,
                                        Distribusjonstidspunkt distribusjonstidspunkt) {
        this.journalpostId = journalpostId;
        this.batchId = batchId;
        this.bestillendeFagsystem = bestillendeFagsystem;
        this.dokumentProdApp = dokumentProdApp;
        this.adresse = adresse;
        this.distribusjonstype = distribusjonstype;
        this.distribusjonstidspunkt = distribusjonstidspunkt;
    }

    public DistribuerJournalpostRequest() {
    }

    public static DistribuerJournalpostRequestBuilder builder() {
        return new DistribuerJournalpostRequestBuilder();
    }

    public String getJournalpostId() {
        return journalpostId;
    }

    public String getBatchId() {
        return batchId;
    }

    public String getBestillendeFagsystem() {
        return bestillendeFagsystem;
    }

    public String getDokumentProdApp() {
        return dokumentProdApp;
    }

    public Adresse getAdresse() {
        return adresse;
    }

    public Distribusjonstype getDistribusjonstype() {
        return distribusjonstype;
    }

    public void setDistribusjonstype(Distribusjonstype distribusjonstype) {
        this.distribusjonstype = distribusjonstype;
    }

    public Distribusjonstidspunkt getDistribusjonstidspunkt() {
        return distribusjonstidspunkt;
    }

    public static class DistribuerJournalpostRequestBuilder {
        private String journalpostId;
        private String batchId;
        private String bestillendeFagsystem;
        private String dokumentProdApp;
        private Adresse adresse;
        private Distribusjonstype distribusjonstype;
        private Distribusjonstidspunkt distribusjonstidspunkt;

        DistribuerJournalpostRequestBuilder() {
        }

        public DistribuerJournalpostRequest.DistribuerJournalpostRequestBuilder journalpostId(String journalpostId) {
            this.journalpostId = journalpostId;
            return this;
        }

        public DistribuerJournalpostRequest.DistribuerJournalpostRequestBuilder batchId(String batchId) {
            this.batchId = batchId;
            return this;
        }

        public DistribuerJournalpostRequest.DistribuerJournalpostRequestBuilder bestillendeFagsystem(String bestillendeFagsystem) {
            this.bestillendeFagsystem = bestillendeFagsystem;
            return this;
        }

        public DistribuerJournalpostRequest.DistribuerJournalpostRequestBuilder dokumentProdApp(String dokumentProdApp) {
            this.dokumentProdApp = dokumentProdApp;
            return this;
        }

        public DistribuerJournalpostRequest.DistribuerJournalpostRequestBuilder adresse(Adresse adresse) {
            this.adresse = adresse;
            return this;
        }

        public DistribuerJournalpostRequest.DistribuerJournalpostRequestBuilder distribusjonstype(Distribusjonstype distribusjonstype) {
            this.distribusjonstype = distribusjonstype;
            return this;
        }

        public DistribuerJournalpostRequest.DistribuerJournalpostRequestBuilder distribusjonstidspunkt(Distribusjonstidspunkt distribusjonstidspunkt) {
            this.distribusjonstidspunkt = distribusjonstidspunkt;
            return this;
        }

        public DistribuerJournalpostRequest build() {
            return new DistribuerJournalpostRequest(journalpostId, batchId, bestillendeFagsystem, dokumentProdApp, adresse, distribusjonstype, distribusjonstidspunkt);
        }
    }
}
