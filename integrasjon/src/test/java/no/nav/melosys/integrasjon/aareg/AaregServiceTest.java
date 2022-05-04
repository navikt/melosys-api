package no.nav.melosys.integrasjon.aareg;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.tomakehurst.wiremock.WireMockServer;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.arbeidsforhold.Arbeidsforhold;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdRestConsumer;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdRestConsumerConfig;
import no.nav.melosys.integrasjon.kodeverk.KodeOppslag;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static no.nav.melosys.domain.FellesKodeverk.PERMISJONS_OG_PERMITTERINGS_BESKRIVELSE;
import static no.nav.melosys.domain.FellesKodeverk.YRKER;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AaregServiceTest {
    private static final String NAV_PERSONIDENT = "12345678990";

    private AaregService aaregService;
    private KodeOppslag mockedKodeOppslag;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private WireMockServer wireMockServer;

    @BeforeAll
    void setupBeforeAll() {
        objectMapper.registerModule(new JavaTimeModule());
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();

        WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:" + wireMockServer.port())
            .filter(ArbeidsforholdRestConsumerConfig.errorFilter())
            .build();
        ArbeidsforholdRestConsumer arbeidsforholdRestConsumer = new ArbeidsforholdRestConsumer(webClient);
        mockedKodeOppslag = Mockito.mock(KodeOppslag.class);
        aaregService = new AaregService(arbeidsforholdRestConsumer, mockedKodeOppslag);
    }

    @AfterAll
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void getArbeidsforholdDokumentFromRestService() throws JsonProcessingException {
        when(mockedKodeOppslag.getTermFraKodeverk(eq(PERMISJONS_OG_PERMITTERINGS_BESKRIVELSE), anyString()))
            .thenReturn("Permisjon med foreldrepenger");
        when(mockedKodeOppslag.getTermFraKodeverk(eq(YRKER), anyString()))
            .thenReturn("IT-KONSULENT");
        wireMockServer.stubFor(get(urlPathEqualTo("/"))
            .withHeader("Nav-Personident", equalTo(NAV_PERSONIDENT))
            .withQueryParam("regelverk", equalTo("A_ORDNINGEN"))
            .withQueryParam("ansettelsesperiodeFom", equalTo("2014-07-01"))
            .withQueryParam("ansettelsesperiodeTom", equalTo("2015-12-31"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody(responsAaregRestBody)
            )
        );
        Saksopplysning saksopplysning = aaregService.finnArbeidsforholdPrArbeidstaker(
            NAV_PERSONIDENT,
            LocalDate.of(2014, 7, 1),
            LocalDate.of(2015, 12, 31));
        ArbeidsforholdDokument arbeidsforholdDokument = (ArbeidsforholdDokument) saksopplysning.getDokument();

        List<Arbeidsforhold> arbeidsforhold = arbeidsforholdDokument.getArbeidsforhold();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.setAnnotationIntrospector(new JacksonAnnotationIntrospector() {
            @Override
            public boolean hasIgnoreMarker(final AnnotatedMember m) {
                // Ikke sjekk disse siden daylight saving vil forandre offset med 1 time og få testen til å feile
                List<String> exclusions = Arrays.asList("opprettelsestidspunkt", "sistBekreftet");
                return exclusions.contains(m.getName()) || super.hasIgnoreMarker(m);
            }
        });
        String result = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(arbeidsforhold);
        Assertions.assertThat(result).isEqualToIgnoringNewLines(expectedRestResult);
    }

    @Test
    void getArbeidsforholdDokumentFromRestService_error_returnererBeskrivendeTekst() {
        wireMockServer.stubFor(get(urlPathEqualTo("/"))
            .willReturn(aResponse()
                .withStatus(500)
                .withHeader("Content-Type", "application/json")
                .withBody("{\"melding\": \"Internal Server Error\"}")
            )
        );

        assertThatExceptionOfType(TekniskException.class)
            .isThrownBy(() -> aaregService.finnArbeidsforholdPrArbeidstaker(
                NAV_PERSONIDENT,
                LocalDate.of(2014, 7, 1),
                LocalDate.of(2015, 12, 31))
            ).withMessageContaining("Henting av arbeidsforhold fra Aareg feilet.");
    }

    private static final String expectedRestResult = """
        [ {
          "arbeidsforholdID" : "abc-321",
          "arbeidsforholdIDnav" : 123456,
          "ansettelsesPeriode" : {
            "fom" : [ 2014, 7, 1 ],
            "tom" : null
          },
          "arbeidsforholdstype" : "ordinaertArbeidsforhold",
          "arbeidsavtaler" : [ {
            "arbeidstidsordning" : {
              "kode" : "ikkeSkift"
            },
            "avloenningstype" : "",
            "yrke" : {
              "kode" : "2130123",
              "term" : "IT-KONSULENT"
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
            "beregnetStillingsprosent" : null,
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
            "permisjonOgPermittering" : "Permisjon med foreldrepenger"
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

    private static final String responsAaregRestBody = """
        [
          {
            "ansettelsesperiode": {
              "bruksperiode": {
                "fom": "2015-01-06T21:44:04.748",
                "tom": "2015-12-06T19:45:04"
              },
              "periode": {
                "fom": "2014-07-01"
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
