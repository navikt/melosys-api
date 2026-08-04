package no.nav.melosys.tjenester.gui.dto.placeholder

import com.fasterxml.jackson.annotation.JsonInclude
import no.nav.melosys.service.placeholder.PlaceholderVerdi

data class PlaceholderVerdierDto(
    val verdier: List<PlaceholderVerdiDto>,
) {
    companion object {
        fun av(verdier: List<PlaceholderVerdi>): PlaceholderVerdierDto =
            PlaceholderVerdierDto(verdier = verdier.map(PlaceholderVerdiDto::av))
    }
}

data class PlaceholderVerdiDto(
    val nokkel: String,
    val verdi: String,
    // Kontrakten krever at feltet er helt fraværende når verdien ikke er flertydig
    @JsonInclude(JsonInclude.Include.NON_NULL)
    val kandidater: List<String>? = null,
) {
    companion object {
        fun av(verdi: PlaceholderVerdi): PlaceholderVerdiDto = PlaceholderVerdiDto(
            nokkel = verdi.nokkel,
            verdi = verdi.verdi,
            kandidater = verdi.kandidater,
        )
    }
}
