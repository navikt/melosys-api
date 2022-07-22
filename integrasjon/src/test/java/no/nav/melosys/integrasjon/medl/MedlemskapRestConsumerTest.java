package no.nav.melosys.integrasjon.medl;

import java.time.LocalDate;

import com.github.tomakehurst.wiremock.WireMockServer;
import no.nav.melosys.integrasjon.medl.api.v1.MedlemskapsunntakForPut;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MedlemskapRestConsumerTest {

    private WireMockServer wireMockServer;
    private MedlemskapRestConsumer restConsumer;

    @BeforeAll
    public void setup() throws Exception {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();

        WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:" + wireMockServer.port())
            .build();

        restConsumer = new MedlemskapRestConsumer(webClient);
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

        assertThat(restConsumer.hentPeriodeListe(fnr, fom, tom)).isEmpty();
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

        assertThat(restConsumer.hentPeriode("123")).isNotNull();
    }

    @Test
    void skalKasteRuntimeExceptionVedOppdatering() {
        wireMockServer.stubFor(put(urlPathEqualTo("/"))
            .willReturn(aResponse()
                .withStatus(400)
                .withBody("Validering feilet")
            )
        );

        assertThatThrownBy(() -> restConsumer.oppdaterPeriode(new MedlemskapsunntakForPut()))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("400 Bad Request from PUT");
    }
}
