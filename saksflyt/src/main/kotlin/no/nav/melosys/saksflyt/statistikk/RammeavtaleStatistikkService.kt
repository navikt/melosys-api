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
     * Henter antall ferdigbehandlede behandlinger der rammeavtale om fjernarbeid (TWFA) er huket av, totalt og
     * fordelt på vedtaksår.
     *
     * Verdien settes kun på [ProsessType.ANMODNING_OM_UNNTAK] og lagres i prosessinstansens prosessdata
     * (java.util.Properties-tekst i CLOB), ikke som egen kolonne. Vi teller derfor behandlinger der prosessdataen
     * inneholder `<kode>=true` for [ProsessDataKey.ER_FJERNARBEID_TWFA].
     *
     * Kun saker som er ferdigbehandlet etter at svar på anmodningen er mottatt telles med, dvs. behandlinger med
     * [Behandlingsresultattyper.FASTSATT_LOVVALGSLAND] og en vedtaksdato. Året er året vedtaket ble fattet.
     *
     * @param fom valgfri fra-og-med-dato (vedtaksdato), null = ingen nedre grense
     * @param tom valgfri til-og-med-dato (vedtaksdato, inklusiv), null = ingen øvre grense
     */
    fun hentRammeavtaleFjernarbeidStatistikk(fom: LocalDate?, tom: LocalDate?): RammeavtaleFjernarbeidStatistikk {
        val prosessType = ProsessType.ANMODNING_OM_UNNTAK.kode
        val fjernarbeidDataMønster = "%${ProsessDataKey.ER_FJERNARBEID_TWFA.kode}=true%"

        val antallPerVedtaksaar = rammeavtaleStatistikkRepository.tellFerdigbehandledePerVedtaksaarMedDataLike(
            prosessType,
            fjernarbeidDataMønster,
            Behandlingsresultattyper.FASTSATT_LOVVALGSLAND.name,
            fom?.atStartOfDay(),
            tom?.plusDays(1)?.atStartOfDay(),
        ).mapNotNull { (aar, antall) -> (aar as? String)?.let { it to (antall as Number).toLong() } }.toMap()

        return RammeavtaleFjernarbeidStatistikk(
            antall = antallPerVedtaksaar.values.sum(),
            fom = fom,
            tom = tom,
            antallPerVedtaksaar = antallPerVedtaksaar,
        )
    }
}
