package no.nav.melosys.saksflyt.steg.fakturering

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype

internal const val DEFAULT_PENSJON_DEKNING_TEKST_HELSEDEL = "Helsedel"

/**
 * Formattert sats-tekst for fakturaperiode-beskrivelse (OEBS-faktura).
 *
 * Returnerer null når sats er null (25%-regel/minstebeløp). Jf. MELOSYS-7981:
 * sats-feltet skal utelates fra beskrivelsen — ingen erstatningstekst — når
 * beløpet er beregnet som totalsum i stedet for via en prosentsats.
 */
internal fun Trygdeavgiftsperiode.satsTekst(): String? =
    trygdesats?.let { "Sats: $it %" }

/** Dekningsbeskrivelse for fakturaperiode. */
internal fun Trygdeavgiftsperiode.dekningTekst(): String {
    val inntektsperiode = hentGrunnlagInntekstperiode()
    if (inntektsperiode.type === Inntektskildetype.PENSJON_UFØRETRYGD ||
        inntektsperiode.type === Inntektskildetype.PENSJON_UFØRETRYGD_KILDESKATT
    ) {
        return DEFAULT_PENSJON_DEKNING_TEKST_HELSEDEL
    }
    return hentGrunnlagMedlemskapsperiode().hentTrygdedekning().beskrivelse
}
