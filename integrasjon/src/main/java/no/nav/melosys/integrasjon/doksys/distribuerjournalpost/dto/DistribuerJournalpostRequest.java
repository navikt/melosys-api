package no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto;

import no.nav.melosys.domain.kodeverk.Distribusjonstyper;

public final class DistribuerJournalpostRequest {

    private String journalpostId;
    private String batchId;
    private String bestillendeFagsystem;
    private String dokumentProdApp;
    private Adresse adresse;
    private Distribusjonstyper distribusjonstype;
    private String distribusjonstidspunkt;

    public DistribuerJournalpostRequest(String journalpostId, String batchId,
                                        String bestillendeFagsystem,
                                        String dokumentProdApp,
                                        Adresse adresse,
                                        Distribusjonstyper distribusjonstype,
                                        String distribusjonstidspunkt) {
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

    public Distribusjonstyper getDistribusjonstype() {
        return distribusjonstype;
    }

    public void setDistribusjonstype(Distribusjonstyper distribusjonstype) {
        this.distribusjonstype = distribusjonstype;
    }

    public String getDistribusjonstidspunkt() {
        return distribusjonstidspunkt;
    }

    public static class DistribuerJournalpostRequestBuilder {
        private String journalpostId;
        private String batchId;
        private String bestillendeFagsystem;
        private String dokumentProdApp;
        private Adresse adresse;
        Distribusjonstyper distribusjonstype;
        String distribusjonstidspunkt;

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

        public DistribuerJournalpostRequest.DistribuerJournalpostRequestBuilder distribusjonstype(Distribusjonstyper distribusjonstype) {
            this.distribusjonstype = distribusjonstype;
            return this;
        }

        public DistribuerJournalpostRequest.DistribuerJournalpostRequestBuilder distribusjonstidspunkt(String distribusjonstidspunkt) {
            this.distribusjonstidspunkt = distribusjonstidspunkt;
            return this;
        }

        public DistribuerJournalpostRequest build() {
            return new DistribuerJournalpostRequest(journalpostId, batchId, bestillendeFagsystem, dokumentProdApp, adresse, distribusjonstype, distribusjonstidspunkt);
        }
    }
}
