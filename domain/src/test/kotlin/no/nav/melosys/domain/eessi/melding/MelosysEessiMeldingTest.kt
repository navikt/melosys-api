package no.nav.melosys.domain.eessi.melding

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import no.nav.melosys.domain.eessi.Periode
import no.nav.melosys.domain.eessi.SvarAnmodningUnntak
import org.junit.jupiter.api.Test
import java.time.LocalDate

class MelosysEessiMeldingTest {

    @Test
    fun `skal takle arbeidssted med null verdi`() {
        val objectMapper = jacksonObjectMapper()

        val json = """
            {
              "arbeidsland": [
                {
                  "land": "Norge",
                  "arbeidssted": null
                }
              ]
            }
        """

        val melding = objectMapper.readValue<MelosysEessiMelding>(json)
        melding.arbeidsland!!.shouldHaveSize(1)
            .single()
            .arbeidssted shouldBe emptyList()
    }

    @Test
    fun `lagUnikIdentifikator bruker alle verdier når de er satt`() {
        val melding = MelosysEessiMelding(
            rinaSaksnummer = "rinaSaksnummer",
            sedId = "SED123",
            sedVersjon = "v4"
        )

        melding.lagUnikIdentifikator() shouldBe "rinaSaksnummer_SED123_v4"
    }


    @Test
    fun `serialisering av fullt populert MelosysEessiMelding gir forventet JSON`() {
        val objectMapper: ObjectMapper = jacksonObjectMapper()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .findAndRegisterModules()


        // Opprett fullt populert objekt
        val melding = MelosysEessiMelding(
            sedId = "SED-2024-001",
            sequenceId = 42,
            rinaSaksnummer = "NO-RINA-2024-12345",
            avsender = Avsender(
                avsenderID = "NO:123456789",
                landkode = "NO"
            ),
            journalpostId = "JP-2024-999",
            dokumentId = "DOK-2024-555",
            gsakSaksnummer = 987654321L,
            aktoerId = "1234567890123",
            statsborgerskap = listOf(
                Statsborgerskap("NO"),
                Statsborgerskap("SE")
            ),
            arbeidssteder = listOf(
                Arbeidssted(
                    navn = "Acme Corp",
                    adresse = Adresse(
                        by = "Oslo",
                        bygning = "Hovedbygget",
                        gate = "Storgata 1",
                        land = "NO",
                        postnummer = "0123",
                        region = "Oslo",
                        type = "kontor"
                    ),
                    hjemmebase = "Oslo",
                    erIkkeFastAdresse = false
                )
            ),
            arbeidsland = listOf(
                Arbeidsland(
                    land = "Norge",
                    arbeidssted = listOf(
                        Arbeidssted(
                            navn = "NAV IT",
                            adresse = Adresse(
                                by = "Oslo",
                                bygning = "Skedsmo",
                                gate = "Sannergata 2",
                                land = "NO",
                                postnummer = "0557",
                                region = "Oslo",
                                type = "kontor"
                            ),
                            hjemmebase = "Oslo",
                            erIkkeFastAdresse = false
                        )
                    )
                )
            ),
            periode = Periode(
                fom = LocalDate.of(2024, 1, 1),
                tom = LocalDate.of(2024, 12, 31)
            ),
            lovvalgsland = "NO",
            artikkel = "11.3.a",
            erEndring = true,
            midlertidigBestemmelse = false,
            x006NavErFjernet = true,
            ytterligereInformasjon = "Dette er ytterligere informasjon om saken",
            bucType = "LA_BUC_01",
            sedType = "A009",
            sedVersjon = "4.2",
            svarAnmodningUnntak = SvarAnmodningUnntak(
                beslutning = SvarAnmodningUnntak.Beslutning.INNVILGELSE,
                begrunnelse = "Innvilget basert på arbeidsforhold",
                delvisInnvilgetPeriode = Periode(
                    fom = LocalDate.of(2024, 6, 1),
                    tom = LocalDate.of(2024, 12, 31)
                )
            ),
            anmodningUnntak = AnmodningUnntak(
                unntakFraLovvalgsland = "SE",
                unntakFraLovvalgsbestemmelse = "Art. 16"
            )
        )

        val actualJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(melding)

        actualJson shouldEqualJson EXPECTED_JSON

        // Verifiser at vi kan deserialisere tilbake
        val deserializedMelding = objectMapper.readValue<MelosysEessiMelding>(EXPECTED_JSON)

        deserializedMelding shouldBe melding
    }

    companion object {
        const val EXPECTED_JSON = """
        {
          "sedId" : "SED-2024-001",
          "sequenceId" : 42,
          "rinaSaksnummer" : "NO-RINA-2024-12345",
          "avsender" : {
            "avsenderID" : "NO:123456789",
            "landkode" : "NO"
          },
          "journalpostId" : "JP-2024-999",
          "dokumentId" : "DOK-2024-555",
          "gsakSaksnummer" : 987654321,
          "aktoerId" : "1234567890123",
          "statsborgerskap" : [ {
            "landkode" : "NO"
          }, {
            "landkode" : "SE"
          } ],
          "arbeidssteder" : [ {
            "navn" : "Acme Corp",
            "adresse" : {
              "by" : "Oslo",
              "bygning" : "Hovedbygget",
              "gate" : "Storgata 1",
              "land" : "NO",
              "postnummer" : "0123",
              "region" : "Oslo",
              "type" : "kontor"
            },
            "hjemmebase" : "Oslo",
            "erIkkeFastAdresse" : false
          } ],
          "arbeidsland" : [ {
            "land" : "Norge",
            "arbeidssted" : [ {
              "navn" : "NAV IT",
              "adresse" : {
                "by" : "Oslo",
                "bygning" : "Skedsmo",
                "gate" : "Sannergata 2",
                "land" : "NO",
                "postnummer" : "0557",
                "region" : "Oslo",
                "type" : "kontor"
              },
              "hjemmebase" : "Oslo",
              "erIkkeFastAdresse" : false
            } ]
          } ],
          "periode" : {
            "fom" : "2024-01-01",
            "tom" : "2024-12-31"
          },
          "lovvalgsland" : "NO",
          "artikkel" : "11.3.a",
          "erEndring" : true,
          "midlertidigBestemmelse" : false,
          "x006NavErFjernet" : true,
          "ytterligereInformasjon" : "Dette er ytterligere informasjon om saken",
          "bucType" : "LA_BUC_01",
          "sedType" : "A009",
          "sedVersjon" : "4.2",
          "svarAnmodningUnntak" : {
            "beslutning" : "INNVILGELSE",
            "begrunnelse" : "Innvilget basert på arbeidsforhold",
            "delvisInnvilgetPeriode" : {
              "fom" : "2024-06-01",
              "tom" : "2024-12-31"
            }
          },
          "anmodningUnntak" : {
            "unntakFraLovvalgsland" : "SE",
            "unntakFraLovvalgsbestemmelse" : "Art. 16"
          }
        }
     """
    }

}
