package no.nav.melosys.itest

import no.nav.melosys.integrasjon.kodeverk.Kode
import no.nav.melosys.integrasjon.kodeverk.Kodeverk
import no.nav.melosys.integrasjon.kodeverk.KodeverkRegister
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import java.time.LocalDate

/**
 * Test-konfigurasjon som erstatter KodeverkRegister med en stub.
 * Dette forhindrer HTTP-kall til kodeverk-endepunktet i integrasjonstester.
 *
 * Stubben returnerer grunnleggende testdata for vanlige kodeverk som
 * Landkoder, Postnummer, etc. Tester som trenger spesifikke kodeverk-data
 * bør sette opp dette selv.
 */
@TestConfiguration
class KodeverkTestConfig {

    @Bean
    @Primary
    fun stubKodeverkRegister(): KodeverkRegister = StubKodeverkRegister()
}

/**
 * Stub-implementasjon av KodeverkRegister som returnerer testdata uten HTTP-kall.
 */
class StubKodeverkRegister : KodeverkRegister {

    private val defaultGyldigFom = LocalDate.of(2000, 1, 1)
    private val defaultGyldigTom = LocalDate.of(2099, 12, 31)

    override fun hentKodeverk(kodeverkNavn: String): Kodeverk {
        val koder = when (kodeverkNavn) {
            "Landkoder" -> lagLandkoder()
            "LandkoderISO2" -> lagLandkoderISO2()
            "Postnummer" -> lagPostnummer()
            "Kjønnstyper" -> lagKjønnstyper()
            "Sivilstander" -> lagSivilstander()
            "Familierelasjoner" -> lagFamilierelasjoner()
            else -> emptyMap()
        }
        return Kodeverk(kodeverkNavn, koder)
    }

    private fun lagLandkoder(): Map<String, List<Kode>> = mapOf(
        "NOR" to listOf(lagKode("NOR", "Norge")),
        "SWE" to listOf(lagKode("SWE", "Sverige")),
        "DNK" to listOf(lagKode("DNK", "Danmark")),
        "FIN" to listOf(lagKode("FIN", "Finland")),
        "DEU" to listOf(lagKode("DEU", "Tyskland")),
        "GBR" to listOf(lagKode("GBR", "Storbritannia")),
        "USA" to listOf(lagKode("USA", "USA")),
        "POL" to listOf(lagKode("POL", "Polen"))
    )

    private fun lagLandkoderISO2(): Map<String, List<Kode>> = mapOf(
        "NO" to listOf(lagKode("NO", "Norge")),
        "SE" to listOf(lagKode("SE", "Sverige")),
        "DK" to listOf(lagKode("DK", "Danmark")),
        "FI" to listOf(lagKode("FI", "Finland")),
        "DE" to listOf(lagKode("DE", "Tyskland")),
        "GB" to listOf(lagKode("GB", "Storbritannia")),
        "US" to listOf(lagKode("US", "USA")),
        "PL" to listOf(lagKode("PL", "Polen"))
    )

    private fun lagPostnummer(): Map<String, List<Kode>> = mapOf(
        "0001" to listOf(lagKode("0001", "OSLO")),
        "0010" to listOf(lagKode("0010", "OSLO")),
        "0102" to listOf(lagKode("0102", "OSLO")),
        "5003" to listOf(lagKode("5003", "BERGEN")),
        "7010" to listOf(lagKode("7010", "TRONDHEIM"))
    )

    private fun lagKjønnstyper(): Map<String, List<Kode>> = mapOf(
        "K" to listOf(lagKode("K", "Kvinne")),
        "M" to listOf(lagKode("M", "Mann"))
    )

    private fun lagSivilstander(): Map<String, List<Kode>> = mapOf(
        "UGIF" to listOf(lagKode("UGIF", "Ugift")),
        "GIFT" to listOf(lagKode("GIFT", "Gift")),
        "SAMB" to listOf(lagKode("SAMB", "Samboer")),
        "SKIL" to listOf(lagKode("SKIL", "Skilt")),
        "ENKE" to listOf(lagKode("ENKE", "Enke/Enkemann"))
    )

    private fun lagFamilierelasjoner(): Map<String, List<Kode>> = mapOf(
        "EKTE" to listOf(lagKode("EKTE", "Ektefelle")),
        "BARN" to listOf(lagKode("BARN", "Barn")),
        "SAMB" to listOf(lagKode("SAMB", "Samboer")),
        "MOR" to listOf(lagKode("MOR", "Mor")),
        "FAR" to listOf(lagKode("FAR", "Far"))
    )

    private fun lagKode(kode: String, navn: String) = Kode(
        kode = kode,
        navn = navn,
        gyldigFom = defaultGyldigFom,
        gyldigTom = defaultGyldigTom
    )
}
