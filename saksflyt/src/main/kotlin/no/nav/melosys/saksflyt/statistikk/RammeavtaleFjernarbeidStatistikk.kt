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
 * @property saker sakene bak tallene, sortert på vedtaksdato. `null` når `inkluderSaksnummer=false`.
 *   Når listen er med, er `saker.size == antall`.
 */
data class RammeavtaleFjernarbeidStatistikk(
    val antall: Long,
    val fom: LocalDate?,
    val tom: LocalDate?,
    val antallPerVedtaksaar: Map<String, Long>,
    val saker: List<RammeavtaleSak>?,
)

/**
 * En enkelt ferdigbehandlet behandling med rammeavtale om fjernarbeid huket av, identifisert med Melosys
 * saksnummer (MEL-nr) for sporbarhet ved spørsmål i enkeltsaker.
 *
 * NB: samme [saksnummer] kan forekomme flere ganger. Én fagsak kan ha flere behandlinger som hver er huket av for
 * rammeavtalen og har eget vedtak, og statistikken teller behandlinger — ikke saker. Skjer det på samme dato blir
 * radene helt like; behandling-id-en som skiller dem er bevisst holdt utenfor responsen, siden den ikke sier
 * Medlemskap og avgift noe. Bruk [RammeavtaleFjernarbeidStatistikk.antall] om du trenger tallet.
 *
 * @property saksnummer Melosys saksnummer, f.eks. `MEL-12345`
 * @property vedtaksaar året vedtaket ble fattet, samme bøtte som i [RammeavtaleFjernarbeidStatistikk.antallPerVedtaksaar]
 * @property vedtaksdato datoen vedtaket ble fattet
 */
data class RammeavtaleSak(
    val saksnummer: String,
    val vedtaksaar: String,
    val vedtaksdato: LocalDate,
)
