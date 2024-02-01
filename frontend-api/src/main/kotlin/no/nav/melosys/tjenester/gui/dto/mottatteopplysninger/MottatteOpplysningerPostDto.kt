package no.nav.melosys.tjenester.gui.dto.mottatteopplysninger

import com.fasterxml.jackson.databind.JsonNode
import io.swagger.annotations.ApiModelProperty

class MottatteOpplysningerPostDto {
    @JvmField
    @ApiModelProperty(dataType = "no.nav.melosys.domain.mottatteopplysninger.MottatteOpplysningerData", required = true)
    var data: JsonNode? = null
}
