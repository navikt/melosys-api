package no.nav.melosys.tjenester.gui.dto.eessi;

import java.util.List;

import no.nav.melosys.domain.eessi.BucType;

public class BucBestillingDto {

    private BucType bucType;
    private String mottakerLand;
    private List<String> mottakerId;

    public BucBestillingDto(BucType bucType, String mottakerLand, List<String> mottakerId) {
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

    public List<String> getMottakerId() {
        return mottakerId;
    }

    public void setMottakerId(List<String> mottakerId) {
        this.mottakerId = mottakerId;
    }
}
