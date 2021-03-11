package no.nav.melosys.integrasjon.avgiftoverforing.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class AvgiftOverforingRepresentantDto {
    private final String id;
    private final String navn;

    @JsonCreator
    public AvgiftOverforingRepresentantDto(@JsonProperty("id") String id,
                                           @JsonProperty("navn") String navn) {
        this.id = id;
        this.navn = navn;
    }

    public String getId() {
        return id;
    }

    public String getNavn() {
        return navn;
    }
}
