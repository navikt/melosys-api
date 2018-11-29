package no.nav.melosys.tjenester.gui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class VedtakDto {

    @JsonProperty("behandlingsresultat")
    public String behandlingsresultatType;

    public String getBehandlingsresultatType() {
        return behandlingsresultatType;
    }

    public void setBehandlingsresultatType(String behandlingsresultatType) {
        this.behandlingsresultatType = behandlingsresultatType;
    }

}
