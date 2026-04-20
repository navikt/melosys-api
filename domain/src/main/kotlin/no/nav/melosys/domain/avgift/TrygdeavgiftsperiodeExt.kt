package no.nav.melosys.domain.avgift

/**
 * Formattert sats-tekst for fakturaperiode-beskrivelse (OEBS-faktura).
 *
 * For 25%-regel og minstebeløp er sats null fordi beløpet beregnes som totalsum,
 * ikke via en prosentsats. Teksten forklarer da hvorfor sats mangler.
 */
fun Trygdeavgiftsperiode.satsTekst(): String =
    trygdesats?.let { "Sats: $it %" } ?: when (beregningsregel) {
        Avgiftsberegningsregel.TJUEFEM_PROSENT_REGEL -> "Begrenset etter 25%-regelen"
        Avgiftsberegningsregel.MINSTEBELØP -> "Under minstebeløp for trygdeavgift"
        else -> "Ingen sats"
    }
