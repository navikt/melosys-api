package no.nav.melosys.integrasjon.gsak.felles.dto;

public class FeilResponseDto {

    private String uuid;
    private String feilmelding;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getFeilmelding() {
        return feilmelding;
    }

    public void setFeilmelding(String feilmelding) {
        this.feilmelding = feilmelding;
    }

}
