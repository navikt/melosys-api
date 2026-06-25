package no.nav.melosys.saksflyt.statistikk

import java.time.LocalDate

/**
 * Statistikk over antall behandlinger (anmodning om unntak) der rammeavtale om fjernarbeid (TWFA) er huket av.
 *
 * @property antall totalt antall i (eventuelt) valgt periode
 * @property fom valgt fra-og-med-dato (null = ingen nedre grense)
 * @property tom valgt til-og-med-dato (null = ingen øvre grense)
 * @property antallPerAar antall fordelt på år (registrert_dato), sortert stigende
 */
data class RammeavtaleFjernarbeidStatistikk(
    val antall: Long,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val antallPerAar: Map<String, Long>,
)
