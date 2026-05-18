package no.nav.melosys.saksflyt.steg.fakturering

import no.nav.melosys.domain.avgift.Trygdeavgiftsperiode
import no.nav.melosys.domain.kodeverk.Inntektskildetype
import no.nav.melosys.integrasjon.faktureringskomponenten.dto.FakturaseriePeriodeDto

internal const val DEFAULT_PENSJON_DEKNING_TEKST_HELSEDEL = "Helsedel"

/**
 * Mapper trygdeavgiftsperioder til [FakturaseriePeriodeDto]-liste for OEBS-faktura.
 *
 * @param perioder         Trygdeavgiftsperioder som skal mappes.
 * @param prefiks          Valgfri førsteledds-tekst i beskrivelsen (f.eks. satsendring-melding).
 * @param inkluderDekning  Om dekningsbeskrivelse skal med i beskrivelsen.
 */
internal fun mapTilFakturaperioder(
    perioder: List<Trygdeavgiftsperiode>,
    prefiks: String? = null,
    inkluderDekning: Boolean = true,
): List<FakturaseriePeriodeDto> = perioder.map { mapTilFakturaperiode(it, prefiks, inkluderDekning) }

private fun mapTilFakturaperiode(
    periode: Trygdeavgiftsperiode,
    prefiks: String?,
    inkluderDekning: Boolean,
) = FakturaseriePeriodeDto(
    enhetsprisPerManed = periode.trygdeavgiftsbeløpMd.hentVerdi(),
    startDato = periode.periodeFra,
    sluttDato = periode.periodeTil,
    beskrivelse = listOfNotNull(
        prefiks,
        "Inntekt: ${periode.hentGrunnlagInntekstperiode().avgiftspliktigMndInntekt.verdi}",
        if (inkluderDekning) "Dekning: ${periode.dekningTekst()}" else null,
        periode.satsTekst(),
    ).joinToString(", ")
)

/**
 * Formattert sats-tekst for fakturaperiode-beskrivelse (OEBS-faktura).
 *
 * Returnerer null når sats er null (25%-regel/minstebeløp). Jf. MELOSYS-7981:
 * sats-feltet skal utelates fra beskrivelsen — ingen erstatningstekst — når
 * beløpet er beregnet som totalsum i stedet for via en prosentsats.
 */
private fun Trygdeavgiftsperiode.satsTekst(): String? =
    trygdesats?.let { "Sats: $it %" }

/** Dekningsbeskrivelse for fakturaperiode. */
private fun Trygdeavgiftsperiode.dekningTekst(): String {
    val inntektsperiode = hentGrunnlagInntekstperiode()
    if (inntektsperiode.type === Inntektskildetype.PENSJON_UFØRETRYGD ||
        inntektsperiode.type === Inntektskildetype.PENSJON_UFØRETRYGD_KILDESKATT
    ) {
        return DEFAULT_PENSJON_DEKNING_TEKST_HELSEDEL
    }
    return hentGrunnlagMedlemskapsperiode().hentTrygdedekning().beskrivelse
}
