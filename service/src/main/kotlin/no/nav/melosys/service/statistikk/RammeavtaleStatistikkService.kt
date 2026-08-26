package no.nav.melosys.service.statistikk

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class RammeavtaleStatistikkService(
    private val rammeavtaleStatistikkRepository: RammeavtaleStatistikkRepository,
) {

    /**
     * Henter ferdigbehandlede behandlinger der rammeavtale om fjernarbeid (TWFA) er huket av, som antall totalt,
     * antall fordelt på vedtaksår og (valgfritt) saksnummeret bak hver behandling.
     *
     * Flagget settes av saksbehandler ved anmodning om unntak og lagres på `anmodningsperiode.er_fjernarbeid_twfa`
     * (se `AnmodningsperiodeService.registrerAnmodning`). Kjent avgrensning: kun saker Norge selv har sendt telles,
     * ikke innkommende A001 (MELOSYS-8252).
     *
     * Kun saker som er ferdigbehandlet etter at svar på anmodningen er mottatt telles med, dvs. behandlinger med
     * [Behandlingsresultattyper.FASTSATT_LOVVALGSLAND] og en vedtaksdato. Året er året vedtaket ble fattet.
     *
     * Både antallene og saksnummerlisten kommer fra samme spørring, slik at de ikke kan komme i utakt.
     * [inkluderSaksnummer] styrer kun hva som tas med i responsen.
     *
     * @param fom valgfri fra-og-med-dato (vedtaksdato), null = ingen nedre grense
     * @param tom valgfri til-og-med-dato (vedtaksdato, inklusiv), null = ingen øvre grense
     * @param inkluderSaksnummer ta med saksnummer (MEL-nr) per behandling i responsen
     */
    fun hentRammeavtaleFjernarbeidStatistikk(
        fom: LocalDate?,
        tom: LocalDate?,
        inkluderSaksnummer: Boolean = true,
    ): RammeavtaleFjernarbeidStatistikk {
        val saker = rammeavtaleStatistikkRepository.finnFerdigbehandledeMedFjernarbeid(
            Behandlingsresultattyper.FASTSATT_LOVVALGSLAND.name,
            fom?.atStartOfDay(),
            tom?.plusDays(1)?.atStartOfDay(),
        ).map { rad -> tilSak(rad) }

        val antallPerVedtaksaar = saker.groupingBy { it.vedtaksaar }
            .eachCount()
            .mapValues { it.value.toLong() }
            .toSortedMap()

        return RammeavtaleFjernarbeidStatistikk(
            antall = saker.size.toLong(),
            fom = fom,
            tom = tom,
            antallPerVedtaksaar = antallPerVedtaksaar,
            saker = if (inkluderSaksnummer) saker else null,
        )
    }

    /**
     * Rad fra [RammeavtaleStatistikkRepository.finnFerdigbehandledeMedFjernarbeid]: `[saksnummer, behandlingId, vedtaksdato]`.
     *
     * `saksnummer` og `vedtaksdato` er non-null ved konstruksjon — `behandling.saksnummer` er NOT NULL i skjemaet og
     * spørringen krever `vm.vedtak_dato IS NOT NULL`. Vi feiler derfor heller enn å hoppe over raden: å slippe en rad
     * i stillhet ville underrapportert et tall som brukes i offisiell rapportering, uten noe signal noe sted.
     *
     * Feilmeldingen peker på behandling-id og ikke saksnummer, fordi den ender i responsbodyen på en HTTP 500.
     * `behandlingId` leses ikke ellers — den er med i spørringen for at SELECT DISTINCT skal skille behandlinger.
     */
    private fun tilSak(rad: Array<Any>): RammeavtaleSak {
        val behandlingId = (rad.getOrNull(1) as? Number)?.toLong()
        val saksnummer = rad.getOrNull(0) as? String
            ?: error("Rad uten saksnummer fra uttrekket for rammeavtale om fjernarbeid (behandling $behandlingId)")
        val vedtaksdato = (rad.getOrNull(2) as? String)?.let { tekst ->
            runCatching { LocalDate.parse(tekst) }.getOrNull()
        } ?: error("Rad uten gyldig vedtaksdato fra uttrekket for rammeavtale om fjernarbeid (behandling $behandlingId)")
        return RammeavtaleSak(
            saksnummer = saksnummer,
            vedtaksaar = vedtaksdato.year.toString(),
            vedtaksdato = vedtaksdato,
        )
    }
}
