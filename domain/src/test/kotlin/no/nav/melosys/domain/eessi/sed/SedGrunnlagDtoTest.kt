package no.nav.melosys.domain.eessi.sed

import com.fasterxml.jackson.databind.ObjectMapper
import io.kotest.assertions.json.shouldEqualJson
import org.junit.jupiter.api.Test

internal class SedGrunnlagDtoTest {

    @Test
    fun serialiserSedDataDto() {
        val sedDataDtoJsonString = ObjectMapper()
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(SedDataDto())

        sedDataDtoJsonString shouldEqualJson EXPECTED_SED_DAYA_DTO_JSON_STRING
    }

    companion object {
        private const val EXPECTED_SED_DAYA_DTO_JSON_STRING = """
            {
              "sedType" : "N/A",
              "sedType" : null,
              "utenlandskIdent" : [ ],
              "bostedsadresse" : null,
              "arbeidsgivendeVirksomheter" : [ ],
              "selvstendigeVirksomheter" : [ ],
              "arbeidssteder" : [ ],
              "arbeidsland" : [ ],
              "harFastArbeidssted" : null,
              "lovvalgsperioder" : [ ],
              "ytterligereInformasjon" : null,
              "bruker" : null,
              "kontaktadresse" : null,
              "oppholdsadresse" : null,
              "familieMedlem" : [ ],
              "søknadsperiode" : null,
              "avklartBostedsland" : null,
              "gsakSaksnummer" : null,
              "tidligereLovvalgsperioder" : [ ],
              "mottakerIder" : null,
              "svarAnmodningUnntak" : null,
              "utpekingAvvis" : null,
              "vedtakDto" : null,
              "invalideringSedDto" : null
            }
        """
    }
}
