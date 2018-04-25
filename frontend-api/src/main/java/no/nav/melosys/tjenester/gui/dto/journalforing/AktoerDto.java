package no.nav.melosys.tjenester.gui.dto.journalforing;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AktoerDto {
    private String id;
    private String navn;

    public AktoerDto(String id, String navn) {
        this.id = id;
        this.navn = navn;
    }

    @JsonProperty("ID")
    public String getId() {
        return id;
    }

    @JsonProperty("ID")
    public void setID(String id) {
        this.id = id;
    }

    public String getNavn() {
        return navn;
    }

    public void setNavn(String navn) {
        this.navn = navn;
    }
}
