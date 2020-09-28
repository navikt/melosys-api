package no.nav.melosys.tjenester.gui.dto.eessi;

import java.util.Collection;

import no.nav.melosys.domain.eessi.BucType;

public class BucBestillingDto {

    private BucType bucType;
    private String mottakerLand;
    private String mottakerId;
    private Collection<VedleggDto> vedlegg;

    public BucBestillingDto(BucType bucType, String mottakerLand, String mottakerId, Collection<VedleggDto> vedlegg) {
        this.bucType = bucType;
        this.mottakerLand = mottakerLand;
        this.mottakerId = mottakerId;
        this.vedlegg = vedlegg;
    }

    public BucType getBucType() {
        return bucType;
    }

    public void setBucType(BucType bucType) {
        this.bucType = bucType;
    }

    public String getMottakerLand() {
        return mottakerLand;
    }

    public void setMottakerLand(String mottakerLand) {
        this.mottakerLand = mottakerLand;
    }

    public String getMottakerId() {
        return mottakerId;
    }

    public void setMottakerId(String mottakerId) {
        this.mottakerId = mottakerId;
    }

    public Collection<VedleggDto> getVedlegg() {
        return vedlegg;
    }

    public void setVedlegg(Collection<VedleggDto> vedlegg) {
        this.vedlegg = vedlegg;
    }
}
