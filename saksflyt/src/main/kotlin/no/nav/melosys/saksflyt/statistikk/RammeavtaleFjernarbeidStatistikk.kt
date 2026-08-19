package no.nav.melosys.saksflyt.statistikk

import java.time.LocalDate

/**
 * Statistikk over antall ferdigbehandlede behandlinger (anmodning om unntak med fastsatt lovvalg) der rammeavtale
 * om fjernarbeid (TWFA) er huket av.
 *
 * @property antall totalt antall i (eventuelt) valgt periode
 * @property fom valgt fra-og-med-dato for vedtaksdato (null = ingen nedre grense)
 * @property tom valgt til-og-med-dato for vedtaksdato (null = ingen øvre grense)
 * @property antallPerVedtaksaar antall fordelt på året vedtaket ble fattet, sortert stigende
 */
data class RammeavtaleFjernarbeidStatistikk(
    val antall: Long,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val antallPerVedtaksaar: Map<String, Long>,
)
