package no.nav.melosys.tjenester.gui.dto.placeholder

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
) {
    companion object {
        fun av(verdi: PlaceholderVerdi): PlaceholderVerdiDto = PlaceholderVerdiDto(
            nokkel = verdi.nokkel,
            verdi = verdi.verdi,
        )
    }
}
