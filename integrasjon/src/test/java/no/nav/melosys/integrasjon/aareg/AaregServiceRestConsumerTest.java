package no.nav.melosys.integrasjon.aareg;

import com.github.tomakehurst.wiremock.WireMockServer;
import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.arbeidsforhold.ArbeidsforholdDokument;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.aareg.arbeidsforhold.ArbeidsforholdRestConsumer;
import org.junit.jupiter.api.*;
import org.springframework.web.reactive.function.client.WebClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

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
    public void getArbeidsforholdDokument() throws Exception {
        Saksopplysning saksopplysning = aaregService.finnArbeidsforholdPrArbeidstaker("99999999991", null, null);
        ArbeidsforholdDokument arbeidsforholdDokument = (ArbeidsforholdDokument) saksopplysning.getDokument();

        arbeidsforholdDokument.getArbeidsforhold();
    }

    @Test
    public void getHistoriskArbeidsforholdDokument() throws Exception {
        Saksopplysning saksopplysning = aaregService.hentArbeidsforholdHistorikk(12608035L);
        ArbeidsforholdDokument arbeidsforholdDokument = (ArbeidsforholdDokument) saksopplysning.getDokument();
        assertThat(arbeidsforholdDokument.getArbeidsforhold().size()).isGreaterThan(0);
        assertThat(arbeidsforholdDokument.getArbeidsforhold().get(0).getArbeidsavtaler().size()).isGreaterThan(1);
    }

    @Test
    public void hentSikkerhetsbegrensetArbeidsforholdHistorikkKasterUnntak() throws Exception {
        AaregService instans = lagAaregService();
        Throwable unntak = catchThrowable(() -> instans.hentArbeidsforholdHistorikk(SIKKERHETSBEGRENSET_ID));
        assertThat(unntak).isInstanceOf(SikkerhetsbegrensningException.class)
            .hasMessageContaining("oppslag av arbeidsforhold");
    }


    private AaregService lagAaregService() {
        FakeUnleash unleash = new FakeUnleash();
        unleash.enable("melosys.aareg.rest");
        // TODO, add mock webClient
        return new AaregService(null, null, restConsumer, unleash);
    }

    private final String responsBody = """
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
