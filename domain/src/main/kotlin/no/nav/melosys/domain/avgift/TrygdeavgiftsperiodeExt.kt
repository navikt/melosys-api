package no.nav.melosys.domain.avgift

/**
 * Formattert sats-tekst for fakturaperiode-beskrivelse (OEBS-faktura).
 *
 * Returnerer null når sats er null (25%-regel/minstebeløp). Jf. MELOSYS-7981:
 * sats-feltet skal utelates fra beskrivelsen — ingen erstatningstekst — når
 * beløpet er beregnet som totalsum i stedet for via en prosentsats.
 */
fun Trygdeavgiftsperiode.satsTekst(): String? =
    trygdesats?.let { "Sats: $it %" }
