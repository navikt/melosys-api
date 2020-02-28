package no.nav.melosys.tjenester.gui.dto.behandlingsgrunnlag;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModelProperty;

public class BehandlingsgrunnlagPostDto {

    @ApiModelProperty(dataType = "no.nav.melosys.domain.behandlingsgrunnlag.BehandlingsgrunnlagData", required = true)
    private JsonNode data;

    public JsonNode getData() {
        return data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }
}
