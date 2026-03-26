package no.nav.melosys.domain.avgift

/** Formattert sats-tekst for fakturaperiode-beskrivelse. */
fun Trygdeavgiftsperiode.satsTekst(): String =
    trygdesats?.let { "Sats: $it %" } ?: when (beregningsregel) {
        Avgiftsberegningsregel.TJUEFEM_PROSENT_REGEL -> "25%-regel"
        Avgiftsberegningsregel.MINSTEBELOEP -> "Under minstebeløp"
        else -> "Ingen sats"
    }
