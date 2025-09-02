package no.nav.melosys.tjenester.gui.dto.periode

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.matchers.nulls.shouldNotBeNull
import no.nav.melosys.domain.kodeverk.InnvilgelsesResultat
import no.nav.melosys.domain.kodeverk.Land_iso2
import no.nav.melosys.domain.kodeverk.Medlemskapstyper
import no.nav.melosys.domain.kodeverk.Trygdedekninger
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Tilleggsbestemmelser_883_2004
import no.nav.melosys.tjenester.gui.dto.periode.LovvalgsperiodeDto.enumVerdiEllerNull
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.LocalDate

class LovvalgsperiodeDtoTest {

    @Test
    fun `mapKonstruktør lager samme objekt som ordinær konstruktør`() {
        val json: Map<String, String> = ObjectMapper().readValue(JSON_EKSEMPEL, object : TypeReference<Map<String, String>>() {})
        val resultat = LovvalgsperiodeDto(json)
        val forventet = lagLovvalgsperiodeDtoFraMap(json)


        assertThat(resultat).usingRecursiveComparison().isEqualTo(forventet)
    }

    @Test
    fun `mapKonstruktør lager samme objekt som ordinær konstruktør uten landkode, medlemskapstype og lovvalgsbestemmelse`() {
        val json: MutableMap<String, String> = ObjectMapper().readValue(JSON_EKSEMPEL, object : TypeReference<MutableMap<String, String>>() {})
        json.remove("lovvalgsland")
        json.remove("medlemskapstype")
        json.remove("lovvalgsbestemmelse")

        val resultat = LovvalgsperiodeDto(json)
        val forventet = lagLovvalgsperiodeDtoFraMap(json)


        assertThat(resultat).usingRecursiveComparison().isEqualTo(forventet)
    }

    private fun lagLovvalgsperiodeDtoFraMap(json: Map<String, String>): LovvalgsperiodeDto =
        LovvalgsperiodeDto(
            null,
            PeriodeDto(LocalDate.parse(json["fomDato"].shouldNotBeNull()), LocalDate.parse(json["tomDato"].shouldNotBeNull())),
            enumVerdiEllerNull(Lovvalgbestemmelser_883_2004::class.java, json["lovvalgsbestemmelse"]),
            Tilleggsbestemmelser_883_2004.valueOf(json["tilleggBestemmelse"]!!),
            enumVerdiEllerNull(Land_iso2::class.java, json["lovvalgsland"]),
            InnvilgelsesResultat.valueOf(json["innvilgelsesResultat"]!!),
            enumVerdiEllerNull(Trygdedekninger::class.java, json["trygdeDekning"]),
            enumVerdiEllerNull(Medlemskapstyper::class.java, json["medlemskapstype"]),
            "20"
        )

    companion object {
        private const val JSON_MAL = "" +
                "{" +
                "  \"fomDato\": \"2019-01-01\"," +
                "  \"tomDato\": \"2020-01-01\"," +
                "  \"lovvalgsbestemmelse\": \"FO_883_2004_ART12_1\"," +
                "  \"tilleggBestemmelse\": \"FO_883_2004_ART11_2\"," +
                "  \"unntakFraBestemmelse\": %s," +
                "  \"innvilgelsesResultat\": \"INNVILGET\"," +
                "  \"lovvalgsland\": \"NO\"," +
                "  \"unntakFraLovvalgsland\": %s," +
                "  \"trygdeDekning\": %s," +
                "  \"medlemskapstype\": \"PLIKTIG\"," +
                "  \"medlemskapsperiodeID\": 20" +
                "}"

        private val JSON_EKSEMPEL = String.format(JSON_MAL, "\"FO_883_2004_ART11_1\"", "\"NO\"", "\"FULL_DEKNING_EOSFO\"")
    }
}
