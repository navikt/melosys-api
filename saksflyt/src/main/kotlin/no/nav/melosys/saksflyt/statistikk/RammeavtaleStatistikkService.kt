package no.nav.melosys.saksflyt.statistikk

import no.nav.melosys.saksflytapi.domain.ProsessDataKey
import no.nav.melosys.saksflytapi.domain.ProsessType
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class RammeavtaleStatistikkService(
    private val rammeavtaleStatistikkRepository: RammeavtaleStatistikkRepository,
) {

    /**
     * Henter antall behandlinger der rammeavtale om fjernarbeid (TWFA) er huket av, totalt og fordelt på år.
     * Verdien settes kun på [ProsessType.ANMODNING_OM_UNNTAK] og lagres i prosessinstansens prosessdata
     * (java.util.Properties-tekst i CLOB), ikke som egen kolonne. Vi teller derfor prosessinstanser der dataen
     * inneholder `<kode>=true` for [ProsessDataKey.ER_FJERNARBEID_TWFA].
     *
     * @param fom valgfri fra-og-med-dato (registrert_dato), null = ingen nedre grense
     * @param tom valgfri til-og-med-dato (registrert_dato, inklusiv), null = ingen øvre grense
     */
    fun hentRammeavtaleFjernarbeidStatistikk(fom: LocalDate?, tom: LocalDate?): RammeavtaleFjernarbeidStatistikk {
        val prosessType = ProsessType.ANMODNING_OM_UNNTAK.kode
        val fjernarbeidDataMønster = "%${ProsessDataKey.ER_FJERNARBEID_TWFA.kode}=true%"

        val antallPerAar = rammeavtaleStatistikkRepository.tellPerAarMedDataLike(
            prosessType,
            fjernarbeidDataMønster,
            fom?.atStartOfDay(),
            tom?.plusDays(1)?.atStartOfDay(),
        ).associate { (aar, antall) -> aar as String to (antall as Number).toLong() }

        return RammeavtaleFjernarbeidStatistikk(
            antall = antallPerAar.values.sum(),
            fom = fom,
            tom = tom,
            antallPerAar = antallPerAar,
        )
    }
}
