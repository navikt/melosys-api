package no.nav.melosys.tjenester.gui.dto.placeholder

import no.nav.melosys.service.placeholder.BetingelseDefinisjon
import no.nav.melosys.service.placeholder.PlaceholderDefinisjon
import no.nav.melosys.service.placeholder.PlaceholderKatalog

data class PlaceholderKatalogDto(
    val placeholdere: List<PlaceholderBeskrivelseDto>,
    val betingelser: List<BetingelseBeskrivelseDto>,
) {
    companion object {
        fun av(katalog: PlaceholderKatalog): PlaceholderKatalogDto = PlaceholderKatalogDto(
            placeholdere = katalog.placeholdere.map(PlaceholderBeskrivelseDto::av),
            betingelser = katalog.betingelser.map(BetingelseBeskrivelseDto::av),
        )
    }
}

data class BetingelseBeskrivelseDto(
    val nokkel: String,
    val visningsnavn: String,
    val beskrivelse: String,
    val sakstyper: List<String>,
) {
    companion object {
        fun av(definisjon: BetingelseDefinisjon): BetingelseBeskrivelseDto = BetingelseBeskrivelseDto(
            nokkel = definisjon.nokkel,
            visningsnavn = definisjon.visningsnavn,
            beskrivelse = definisjon.beskrivelse,
            sakstyper = definisjon.sakstyper.map { it.kode },
        )
    }
}

data class PlaceholderBeskrivelseDto(
    val nokkel: String,
    val visningsnavn: String,
    val beskrivelse: String,
    val eksempel: String,
    // Koder, ikke rå enum: KodeSerializer ville ellers gitt {kode, term}-objekter.
    val sakstyper: List<String>,
) {
    companion object {
        fun av(definisjon: PlaceholderDefinisjon): PlaceholderBeskrivelseDto = PlaceholderBeskrivelseDto(
            nokkel = definisjon.nokkel,
            visningsnavn = definisjon.visningsnavn,
            beskrivelse = definisjon.beskrivelse,
            eksempel = definisjon.eksempel(),
            sakstyper = definisjon.sakstyper.map { it.kode },
        )
    }
}
