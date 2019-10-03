package no.nav.melosys.domain.arkiv;

import java.util.List;

public class OpprettJournalpost extends Journalpost {

    private String journalførendeEnhet;
    private FysiskDokument hoveddokument;
    private List<FysiskDokument> vedlegg;

    public OpprettJournalpost() {
        super(null);
    }

    public String getJournalførendeEnhet() {
        return journalførendeEnhet;
    }

    public void setJournalførendeEnhet(String journalførendeEnhet) {
        this.journalførendeEnhet = journalførendeEnhet;
    }

    @Override
    public FysiskDokument getHoveddokument() {
        return hoveddokument;
    }

    public void setHoveddokument(FysiskDokument hoveddokument) {
        this.hoveddokument = hoveddokument;
    }

    public List<FysiskDokument> getVedlegg() {
        return vedlegg;
    }

    public void setVedlegg(List<FysiskDokument> vedlegg) {
        this.vedlegg = vedlegg;
    }
}
