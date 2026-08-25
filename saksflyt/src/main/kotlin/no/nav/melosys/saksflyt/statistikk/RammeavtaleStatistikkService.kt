package no.nav.melosys.saksflyt.statistikk

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper
import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessType
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
     * Verdien settes kun på [ProsessType.ANMODNING_OM_UNNTAK] og lagres i prosessinstansens prosessdata
     * (java.util.Properties-tekst i CLOB), ikke som egen kolonne. Vi teller derfor behandlinger der prosessdataen
     * inneholder `<kode>=true` for [ProsessDataKey.ER_FJERNARBEID_TWFA].
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
        val prosessType = ProsessType.ANMODNING_OM_UNNTAK.kode
        val fjernarbeidDataMønster = "%${ProsessDataKey.ER_FJERNARBEID_TWFA.kode}=true%"

        val saker = rammeavtaleStatistikkRepository.finnFerdigbehandledeMedDataLike(
            prosessType,
            fjernarbeidDataMønster,
            Behandlingsresultattyper.FASTSATT_LOVVALGSLAND.name,
            fom?.atStartOfDay(),
            tom?.plusDays(1)?.atStartOfDay(),
        ).mapNotNull { rad -> tilSak(rad) }

        // LinkedHashMap-rekkefølge: spørringen sorterer på vedtaksdato, så årene kommer stigende
        val antallPerVedtaksaar = saker.groupingBy { it.vedtaksaar }.eachCount().mapValues { it.value.toLong() }

        return RammeavtaleFjernarbeidStatistikk(
            antall = saker.size.toLong(),
            fom = fom,
            tom = tom,
            antallPerVedtaksaar = antallPerVedtaksaar,
            saker = saker.takeIf { inkluderSaksnummer },
        )
    }

    /** Rad fra [RammeavtaleStatistikkRepository.finnFerdigbehandledeMedDataLike]: `[saksnummer, behandlingId, vedtaksdato]`. */
    private fun tilSak(rad: Array<Any>): RammeavtaleSak? {
        val saksnummer = rad.getOrNull(0) as? String ?: return null
        val vedtaksdato = (rad.getOrNull(2) as? String)?.let(LocalDate::parse) ?: return null
        return RammeavtaleSak(
            saksnummer = saksnummer,
            vedtaksaar = vedtaksdato.year.toString(),
            vedtaksdato = vedtaksdato,
        )
    }
}
