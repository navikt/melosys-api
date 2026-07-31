package no.nav.melosys.tjenester.gui.dto.placeholder

import no.nav.melosys.domain.kodeverk.Sakstyper
import no.nav.melosys.service.placeholder.PlaceholderDefinisjon

data class PlaceholderKatalogDto(
    val placeholdere: List<PlaceholderBeskrivelseDto>,
) {
    companion object {
        fun av(definisjoner: List<PlaceholderDefinisjon>): PlaceholderKatalogDto =
            PlaceholderKatalogDto(placeholdere = definisjoner.map(PlaceholderBeskrivelseDto::av))
    }
}

data class PlaceholderBeskrivelseDto(
    val nokkel: String,
    val visningsnavn: String,
    val beskrivelse: String,
    val eksempel: String,
    val sakstyper: List<Sakstyper>,
) {
    companion object {
        fun av(definisjon: PlaceholderDefinisjon): PlaceholderBeskrivelseDto = PlaceholderBeskrivelseDto(
            nokkel = definisjon.nokkel,
            visningsnavn = definisjon.visningsnavn,
            beskrivelse = definisjon.beskrivelse,
            eksempel = definisjon.eksempel(),
            sakstyper = definisjon.sakstyper,
        )
    }
}
