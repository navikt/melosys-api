package no.nav.melosys.integrasjon.aareg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdRestConsumer;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class AaregServiceRestConsumerTest {
    private static final Long SIKKERHETSBEGRENSET_ID = 1L;

    private AaregService aaregService;
    private WireMockServer wireMockServer;
    private ArbeidsforholdRestConsumer restConsumer;

    @BeforeAll
    public void setupBeforeAll() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();

        WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:" + wireMockServer.port())
            .build();
        restConsumer = new ArbeidsforholdRestConsumer(webClient);

        wireMockServer.stubFor(get(urlPathEqualTo("/"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(responsBody)
            )
        );
    }

    @BeforeEach
    public void setUp() {
        aaregService = lagAaregService();
    }

    @AfterAll
    public void tearDown() {
        wireMockServer.stop();
    }

    @Test
    public void getArbeidsforholdDokument() throws JsonProcessingException {
        Saksopplysning saksopplysning = aaregService.finnArbeidsforholdPrArbeidstaker(
            "abc-321",
            LocalDate.of(2014, 7, 1),
            LocalDate.of(2015, 12, 31));
        ArbeidsforholdDokument arbeidsforholdDokument = (ArbeidsforholdDokument) saksopplysning.getDokument();

        List<Arbeidsforhold> arbeidsforhold = arbeidsforholdDokument.getArbeidsforhold();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        String result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(arbeidsforhold);
        assertThat(result).isEqualTo(expectedResult);
    }

    private final String expectedResult = """
        [ {
          "arbeidsforholdID" : "abc-321",
          "arbeidsforholdIDnav" : 123456,
          "ansettelsesPeriode" : {
            "fom" : [ 2014, 7, 1 ],
            "tom" : [ 2015, 12, 31 ]
          },
          "arbeidsforholdstype" : "ordinaertArbeidsforhold",
          "arbeidsavtaler" : [ {
            "arbeidstidsordning" : {
              "kode" : "ikkeSkift"
            },
            "avloenningstype" : "",
            "yrke" : {
              "kode" : "2130123",
              "term" : ""
            },
            "gyldighetsperiode" : {
              "fom" : [ 2014, 7, 1 ],
              "tom" : [ 2015, 12, 31 ]
            },
            "avtaltArbeidstimerPerUke" : 37.5,
            "stillingsprosent" : 49.5,
            "sisteLoennsendringsdato" : [ 2014, 7, 1 ],
            "beregnetAntallTimerPrUke" : 26.5,
            "endringsdatoStillingsprosent" : [ 2014, 7, 1 ],
            "skipsregister" : null,
            "skipstype" : null,
            "maritimArbeidsavtale" : false,
            "beregnetStillingsprosent" : 0.7,
            "antallTimerGammeltAa" : null,
            "fartsomraade" : null
          } ],
          "permisjonOgPermittering" : [ {
            "permisjonsId" : "123-xyz",
            "permisjonsPeriode" : {
              "fom" : [ 2014, 7, 1 ],
              "tom" : [ 2015, 12, 31 ]
            },
            "permisjonsprosent" : 50.5,
            "permisjonOgPermittering" : "permisjonMedForeldrepenger"
          } ],
          "utenlandsopphold" : [ {
            "periode" : {
              "fom" : [ 2014, 7, 1 ],
              "tom" : [ 2015, 12, 31 ]
            },
            "land" : "JPN",
            "rapporteringsAarMaaned" : [ 2017, 12 ]
          } ],
          "arbeidsgivertype" : "ORGANISASJON",
          "arbeidsgiverID" : "991609407",
          "arbeidstakerID" : "31126700000",
          "opplysningspliktigtype" : "ORGANISASJON",
          "opplysningspliktigID" : "991609407",
          "opprettelsestidspunkt" : 1537269149.000000000,
          "sistBekreftet" : 1537359031.000000000,
          "Aordning" : false,
          "timerTimelonnet" : [ {
            "antallTimer" : 37.5,
            "timelonnetPeriode" : {
              "fom" : [ 2014, 7, 1 ],
              "tom" : [ 2015, 12, 31 ]
            },
            "rapporteringsAarMaaned" : [ 2018, 5 ]
          } ]
        } ]""";

    private AaregService lagAaregService() {
        FakeUnleash unleash = new FakeUnleash();
        unleash.enable("melosys.aareg.rest");
        return new AaregService(null, null, restConsumer, unleash);
    }

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
                "beregnetAntallTimerPrUke": 26.5,
                "bruksperiode": {
                  "fom": "2015-01-06T21:44:04.748",
                  "tom": "2015-12-06T19:45:04"
                },
                "gyldighetsperiode": {
                  "fom": "2014-07-01",
                  "tom": "2015-12-31"
                },
                "sistLoennsendring": "2014-07-01",
                "sistStillingsendring": "2014-07-01",
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
              "type": "Organisasjon",
              "organisasjonsnummer": "991609407"
            },
            "arbeidstaker": {
              "aktoerId": "1234567890",
              "offentligIdent": "31126700000"
            },
            "innrapportertEtterAOrdningen": false,
            "navArbeidsforholdId": 123456,
            "opplysningspliktig": {
              "type": "Organisasjon",
              "organisasjonsnummer": "991609407"
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
