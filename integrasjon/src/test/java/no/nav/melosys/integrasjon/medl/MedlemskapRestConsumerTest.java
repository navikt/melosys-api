package no.nav.melosys.integrasjon.medl;

import java.time.LocalDate;
import java.util.List;

import com.github.tomakehurst.wiremock.WireMockServer;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import no.nav.tjenester.medlemskapsunntak.api.v1.MedlemskapsunntakForGet;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MedlemskapRestConsumerTest {

    private WireMockServer wireMockServer;
    private MedlemskapRestConsumer restConsumer;

    private RestStsClient mockRestStsClient = mock(RestStsClient.class);

    @BeforeAll
    public void setup() throws Exception {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();

        when(mockRestStsClient.collectToken()).thenReturn("dummyToken");

        restConsumer = new MedlemskapRestConsumer("http://localhost:" + wireMockServer.port(), mockRestStsClient);
    }

    @AfterAll
    public void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void skalHenteMedlemskapsperiodeliste() {
        LocalDate fom = LocalDate.now().minusDays(2);
        LocalDate tom = LocalDate.now().plusDays(2);
        String fnr = "12345678990";

        wireMockServer.stubFor(get(urlPathEqualTo("/"))
            .withHeader("Nav-Personident", equalTo(fnr))
            .withQueryParam("fraOgMed", equalTo(fom.toString()))
            .withQueryParam("tilOgMed", equalTo(tom.toString()))
            .withQueryParam("inkluderSporingsinfo", equalTo("true"))
            .withQueryParam("ekskluderKilder", equalTo(""))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("[]")
            )
        );
        List<MedlemskapsunntakForGet> response = restConsumer.hentPeriodeListe(fnr, fom, tom);

        assertTrue(response.isEmpty());
    }

    @Test
    void skalHenteEnMedlemskapsperiode() {

        wireMockServer.stubFor(get(urlPathEqualTo("/123"))
            .withQueryParam("inkluderSporingsinfo", equalTo("true"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader("Content-Type", "application/json")
                .withBody("{}")
            )
        );
        MedlemskapsunntakForGet medlemskapsunntak = restConsumer.hentPeriode("123");

        assertNotNull(medlemskapsunntak);
    }
}