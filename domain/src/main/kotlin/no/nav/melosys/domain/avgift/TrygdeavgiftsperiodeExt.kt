package no.nav.melosys.domain.avgift

/** Formattert sats-tekst for fakturaperiode-beskrivelse. */
fun Trygdeavgiftsperiode.satsTekst(): String =
    trygdesats?.let { "Sats: $it %" } ?: "Beregningstype: $beregningstype"
