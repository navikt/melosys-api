package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ArbeidsforholdRestConsumerTest {

    private WireMockServer wireMockServer;
    private ArbeidsforholdRestConsumer restConsumer;

    @BeforeAll
    public void setup() throws Exception {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();

        WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:" + wireMockServer.port())
            .build();

        restConsumer = new ArbeidsforholdRestConsumer(webClient);
    }

    @AfterAll
    public void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void skalFinneArbeidsforholdPrArbeidstaker() throws JsonProcessingException {
        String fnr = "12345678990";

        wireMockServer.stubFor(get(urlPathEqualTo("/"))
            .withHeader("Nav-Personident", equalTo(fnr))
            .withQueryParam("regelverk", equalTo("ALLE"))
            .withQueryParam("ansettelsesperiodeFom", equalTo("2020-01-01"))
            .withQueryParam("ansettelsesperiodeTom", equalTo("2021-01-01"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(responsBody)
            )
        );

        ArbeidsforholdQuery arbeidsforholdQuery = new ArbeidsforholdQuery
            .Builder()
            .regelverk(ArbeidsforholdQuery.Regelverk.ALLE)
            .arbeidsforholdType(ArbeidsforholdQuery.ArbeidsforholdType.ALLE)
            .ansettelsesperiodeFom(LocalDate.parse("2020-01-01"))
            .ansettelsesperiodeTom(LocalDate.parse("2021-01-01"))
            .build();
        ArbeidsforholdResponse arbeidsforholdResponse = restConsumer.finnArbeidsforholdPrArbeidstaker(fnr, arbeidsforholdQuery);

        List<ArbeidsforholdResponse.Arbeidsforhold> arbeidsforhold = arbeidsforholdResponse.getArbeidsforhold();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(arbeidsforhold);
        assertThat(result).isEqualTo(expectedResult);
    }

    private final String expectedResult = """
        [ {
          "arbeidsforholdId" : "abc-321",
          "navArbeidsforholdId" : 123456,
          "ansettelsesperiode" : {
            "periode" : {
              "fom" : "2014-07-01",
              "tom" : "2015-12-31"
            }
          },
          "type" : "ordinaertArbeidsforhold",
          "arbeidstaker" : {
            "type" : null,
            "offentligIdent" : "31126700000",
            "aktoerId" : "1234567890"
          },
          "arbeidsavtaler" : [ {
            "type" : "Forenklet",
            "arbeidstidsordning" : "ikkeSkift",
            "yrke" : "2130123",
            "stillingsprosent" : 49.5,
            "beregnetAntallTimerPrUke" : 37.5,
            "gyldighetsperiode" : {
              "fom" : "2014-07-01",
              "tom" : "2015-12-31"
            },
            "sistStillingsendring" : "string",
            "sistLoennsendring" : "string",
            "antallTimerPrUke" : 37.5
          } ],
          "permisjonPermitteringer" : [ {
            "periode" : {
              "fom" : "2014-07-01",
              "tom" : "2015-12-31"
            },
            "permisjonPermitteringId" : "123-xyz",
            "prosent" : 50.5,
            "type" : "permisjonMedForeldrepenger",
            "varslingskode" : "string"
          } ],
          "utenlandsopphold" : [ {
            "landkode" : "JPN",
            "periode" : {
              "fom" : "2014-07-01",
              "tom" : "2015-12-31"
            },
            "rapporteringsperiode" : "2017-12"
          } ],
          "arbeidsgiver" : {
            "type" : "Organisasjon",
            "organisasjonsnummer" : null
          },
          "opplysningspliktig" : {
            "type" : "Organisasjon",
            "organisasjonsnummer" : null
          },
          "innrapportertEtterAOrdningen" : false,
          "registrert" : "2018-09-18T11:12:29",
          "sistBekreftet" : "2018-09-19T12:10:31",
          "antallTimerForTimeloennet" : [ {
            "antallTimer" : 37.5,
            "periode" : {
              "fom" : "2014-07-01",
              "tom" : "2015-12-31"
            },
            "rapporteringsperiode" : "2018-05"
          } ]
        } ]""";

    private final String responsBody = """
        [
          {
            "ansettelsesperiode": {
              "bruksperiode": {
                "fom": "2015-01-06T21:44:04.748",
                "tom": "2015-12-06T19:45:04"
              },
              "periode": {
                "fom": "2014-07-01",
                "tom": "2015-12-31"
              },
              "sluttaarsak": "arbeidstakerHarSagtOppSelv",
              "sporingsinformasjon": {
                "endretAv": "Z990693",
                "endretKilde": "AAREG",
                "endretKildereferanse": "referanse-fra-kilde",
                "endretTidspunkt": "2018-09-19T12:11:20.79",
                "opprettetAv": "srvappserver",
                "opprettetKilde": "EDAG",
                "opprettetKildereferanse": "22a26849-aeef-4b81-9174-e238c11e1081",
                "opprettetTidspunkt": "2018-09-19T12:10:58.059"
              },
              "varslingskode": "ERKONK"
            },
            "antallTimerForTimeloennet": [
              {
                "antallTimer": 37.5,
                "periode": {
                  "fom": "2014-07-01",
                  "tom": "2015-12-31"
                },
                "rapporteringsperiode": "2018-05",
                "sporingsinformasjon": {
                  "endretAv": "Z990693",
                  "endretKilde": "AAREG",
                  "endretKildereferanse": "referanse-fra-kilde",
                  "endretTidspunkt": "2018-09-19T12:11:20.79",
                  "opprettetAv": "srvappserver",
                  "opprettetKilde": "EDAG",
                  "opprettetKildereferanse": "22a26849-aeef-4b81-9174-e238c11e1081",
                  "opprettetTidspunkt": "2018-09-19T12:10:58.059"
                }
              }
            ],
            "arbeidsavtaler": [
              {
                "ansettelsesform": "fast",
                "antallTimerPrUke": 37.5,
                "arbeidstidsordning": "ikkeSkift",
                "beregnetAntallTimerPrUke": 37.5,
                "bruksperiode": {
                  "fom": "2015-01-06T21:44:04.748",
                  "tom": "2015-12-06T19:45:04"
                },
                "gyldighetsperiode": {
                  "fom": "2014-07-01",
                  "tom": "2015-12-31"
                },
                "sistLoennsendring": "string",
                "sistStillingsendring": "string",
                "sporingsinformasjon": {
                  "endretAv": "Z990693",
                  "endretKilde": "AAREG",
                  "endretKildereferanse": "referanse-fra-kilde",
                  "endretTidspunkt": "2018-09-19T12:11:20.79",
                  "opprettetAv": "srvappserver",
                  "opprettetKilde": "EDAG",
                  "opprettetKildereferanse": "22a26849-aeef-4b81-9174-e238c11e1081",
                  "opprettetTidspunkt": "2018-09-19T12:10:58.059"
                },
                "stillingsprosent": 49.5,
                "type": "Forenklet",
                "yrke": "2130123"
              }
            ],
            "arbeidsforholdId": "abc-321",
            "arbeidsgiver": {
              "type": "Organisasjon"
            },
            "arbeidstaker": {
              "aktoerId": "1234567890",
              "offentligIdent": "31126700000"
            },
            "innrapportertEtterAOrdningen": false,
            "navArbeidsforholdId": 123456,
            "opplysningspliktig": {
              "type": "Organisasjon"
            },
            "permisjonPermitteringer": [
              {
                "periode": {
                  "fom": "2014-07-01",
                  "tom": "2015-12-31"
                },
                "permisjonPermitteringId": "123-xyz",
                "prosent": 50.5,
                "sporingsinformasjon": {
                  "endretAv": "Z990693",
                  "endretKilde": "AAREG",
                  "endretKildereferanse": "referanse-fra-kilde",
                  "endretTidspunkt": "2018-09-19T12:11:20.79",
                  "opprettetAv": "srvappserver",
                  "opprettetKilde": "EDAG",
                  "opprettetKildereferanse": "22a26849-aeef-4b81-9174-e238c11e1081",
                  "opprettetTidspunkt": "2018-09-19T12:10:58.059"
                },
                "type": "permisjonMedForeldrepenger",
                "varslingskode": "string"
              }
            ],
            "registrert": "2018-09-18T11:12:29",
            "sistBekreftet": "2018-09-19T12:10:31",
            "sporingsinformasjon": {
              "endretAv": "Z990693",
              "endretKilde": "AAREG",
              "endretKildereferanse": "referanse-fra-kilde",
              "endretTidspunkt": "2018-09-19T12:11:20.79",
              "opprettetAv": "srvappserver",
              "opprettetKilde": "EDAG",
              "opprettetKildereferanse": "22a26849-aeef-4b81-9174-e238c11e1081",
              "opprettetTidspunkt": "2018-09-19T12:10:58.059"
            },
            "type": "ordinaertArbeidsforhold",
            "utenlandsopphold": [
              {
                "landkode": "JPN",
                "periode": {
                  "fom": "2014-07-01",
                  "tom": "2015-12-31"
                },
                "rapporteringsperiode": "2017-12",
                "sporingsinformasjon": {
                  "endretAv": "Z990693",
                  "endretKilde": "AAREG",
                  "endretKildereferanse": "referanse-fra-kilde",
                  "endretTidspunkt": "2018-09-19T12:11:20.79",
                  "opprettetAv": "srvappserver",
                  "opprettetKilde": "EDAG",
                  "opprettetKildereferanse": "22a26849-aeef-4b81-9174-e238c11e1081",
                  "opprettetTidspunkt": "2018-09-19T12:10:58.059"
                }
              }
            ],
            "varsler": [
              {
                "entitet": "ANSETTELSESPERIODE",
                "varslingskode": "string"
              }
            ]
          }
        ]
        """;

}
