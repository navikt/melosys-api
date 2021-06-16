package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

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
    void skalFinneArbeidsforholdPrArbeidstaker() {
        String fnr = "12345678990";

        wireMockServer.stubFor(get(urlPathEqualTo("/"))
            .withHeader("Nav-Personident", equalTo(fnr))
            .withQueryParam("regelverk", equalTo("ALLE"))
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
            .build();
        ArbeidsforholdResponse arbeidsforholdResponse = restConsumer.finnArbeidsforholdPrArbeidstaker(fnr, arbeidsforholdQuery);

        ArbeidsforholdResponse.Arbeidsforhold[] arbeidsforholds = arbeidsforholdResponse.getArbeidsforhold();
        assertThat(arbeidsforholds.length).isEqualTo(1);
        ArbeidsforholdResponse.Arbeidsforhold arbeidsforhold = arbeidsforholds[0];
        assertThat(arbeidsforhold.getNavArbeidsforholdId()).isEqualTo(3065458);

        ArbeidsforholdResponse.Arbeidstaker arbeidstaker = arbeidsforhold.getArbeidstaker();
        assertThat(arbeidstaker.getType()).isEqualTo("Person");
        assertThat(arbeidstaker.getAktoerId()).isEqualTo("1685359155300");
    }

    String responsBody = """
        [
            {
                "navArbeidsforholdId": 3065458,
                "arbeidstaker": {
                    "type": "Person",
                    "offentligIdent": "64068648643",
                    "aktoerId": "1685359155300"
                },
                "arbeidsgiver": {
                    "type": "Organisasjon",
                    "organisasjonsnummer": "990013608"
                },
                "opplysningspliktig": {
                    "type": "Organisasjon",
                    "organisasjonsnummer": "991609407"
                },
                "type": "ordinaertArbeidsforhold",
                "ansettelsesperiode": {
                    "periode": {
                        "fom": "2021-01-01"
                    },
                    "bruksperiode": {
                        "fom": "2021-03-16T08:49:54.329"
                    },
                    "sporingsinformasjon": {
                        "opprettetTidspunkt": "2021-03-16T08:49:54.33",
                        "opprettetAv": "Z990860",
                        "opprettetKilde": "AAREG",
                        "opprettetKildereferanse": "brevtest",
                        "endretTidspunkt": "2021-03-16T08:49:54.33",
                        "endretAv": "Z990860",
                        "endretKilde": "AAREG",
                        "endretKildereferanse": "brevtest"
                    }
                },
                "arbeidsavtaler": [
                    {
                        "type": "Ordinaer",
                        "arbeidstidsordning": "ikkeSkift",
                        "yrke": "3113145",
                        "stillingsprosent": 100.0,
                        "antallTimerPrUke": 37.5,
                        "beregnetAntallTimerPrUke": 37.5,
                        "bruksperiode": {
                            "fom": "2021-03-16T08:49:54.329"
                        },
                        "gyldighetsperiode": {
                            "fom": "2021-01-01"
                        },
                        "sporingsinformasjon": {
                            "opprettetTidspunkt": "2021-03-16T08:49:54.33",
                            "opprettetAv": "Z990860",
                            "opprettetKilde": "AAREG",
                            "opprettetKildereferanse": "brevtest",
                            "endretTidspunkt": "2021-03-16T08:49:54.33",
                            "endretAv": "Z990860",
                            "endretKilde": "AAREG",
                            "endretKildereferanse": "brevtest"
                        }
                    }
                ],
                "varsler": [
                    {
                        "entitet": "ARBEIDSFORHOLD",
                        "varslingskode": "NAVEND"
                    }
                ],
                "innrapportertEtterAOrdningen": true,
                "registrert": "2021-03-16T08:49:54.267",
                "sistBekreftet": "2021-03-16T08:49:54",
                "sporingsinformasjon": {
                    "opprettetTidspunkt": "2021-03-16T08:49:54.33",
                    "opprettetAv": "Z990860",
                    "opprettetKilde": "AAREG",
                    "opprettetKildereferanse": "brevtest",
                    "endretTidspunkt": "2021-03-16T08:49:54.33",
                    "endretAv": "Z990860",
                    "endretKilde": "AAREG",
                    "endretKildereferanse": "brevtest"
                }
            }
        ]""";

}
