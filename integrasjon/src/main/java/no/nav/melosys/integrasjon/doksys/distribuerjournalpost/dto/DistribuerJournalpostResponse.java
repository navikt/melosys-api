package no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto;

public final class DistribuerJournalpostResponse {

    private final String bestillingId;

    public DistribuerJournalpostResponse(String bestillingId) {
        this.bestillingId = bestillingId;
    }

    public String getBestillingId() {
        return bestillingId;
    }
}
