package no.nav.melosys.tjenester.gui.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TidligereMedlemsperioderDto {

    @JsonProperty("tidligere_medlemsperiode_ids")
    public List<Long> periodeIder;
}
