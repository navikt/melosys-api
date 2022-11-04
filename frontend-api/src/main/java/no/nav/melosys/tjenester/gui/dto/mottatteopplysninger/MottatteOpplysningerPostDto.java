package no.nav.melosys.tjenester.gui.dto.mottatteopplysninger;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.annotations.ApiModelProperty;

public class MottatteOpplysningerPostDto {

    @ApiModelProperty(dataType = "no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData", required = true)
    private JsonNode data;

    public JsonNode getData() {
        return data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }
}
