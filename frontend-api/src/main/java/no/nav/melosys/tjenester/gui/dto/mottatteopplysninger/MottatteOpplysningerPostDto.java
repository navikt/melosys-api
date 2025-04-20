package no.nav.melosys.tjenester.gui.dto.mottatteopplysninger;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

public class MottatteOpplysningerPostDto {

    @Schema(implementation = no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData.class, requiredMode = Schema.RequiredMode.REQUIRED)
    private JsonNode data;

    public JsonNode getData() {
        return data;
    }

    public void setData(JsonNode data) {
        this.data = data;
    }
}
