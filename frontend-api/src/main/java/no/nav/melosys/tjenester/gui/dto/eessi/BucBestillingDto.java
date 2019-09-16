package no.nav.melosys.tjenester.gui.dto.eessi;

import no.nav.melosys.domain.eessi.BucType;

public class BucBestillingDto {

    private BucType bucType;
    private String mottakerLand;
    private String mottakerId;

    public BucBestillingDto(BucType bucType, String mottakerLand, String mottakerId) {
        this.bucType = bucType;
        this.mottakerLand = mottakerLand;
        this.mottakerId = mottakerId;
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
}
