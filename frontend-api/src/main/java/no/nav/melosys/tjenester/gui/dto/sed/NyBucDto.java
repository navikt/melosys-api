package no.nav.melosys.tjenester.gui.dto.sed;

public class NyBucDto {

    private String bucType;
    private String mottakerLand;
    private String mottakerId;

    public NyBucDto(String bucType, String mottakerLand, String mottakerId) {
        this.bucType = bucType;
        this.mottakerLand = mottakerLand;
        this.mottakerId = mottakerId;
    }

    public String getBucType() {
        return bucType;
    }

    public void setBucType(String bucType) {
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
