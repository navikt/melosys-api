package no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto;

public final class DistribuerJournalpostResponse {

    private String bestillingsId;

    public DistribuerJournalpostResponse() {
    }

    public DistribuerJournalpostResponse(String bestillingsId) {
        this.bestillingsId = bestillingsId;
    }

    public String getBestillingsId() {
        return bestillingsId;
    }
}
