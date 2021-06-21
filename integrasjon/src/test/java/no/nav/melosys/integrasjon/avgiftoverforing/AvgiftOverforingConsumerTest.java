package no.nav.melosys.integrasjon.avgiftoverforing;

import no.nav.melosys.integrasjon.avgiftoverforing.dto.AvgiftOverforingRepresentantDto;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

class AvgiftOverforingConsumerTest {

    private static MockWebServer mockWebServer;

    private AvgiftOverforingConsumer avgiftOverforingConsumer;

    @BeforeAll
    static void setupServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @BeforeEach
    void setup() {

        WebClient webClient = WebClient.builder()
            .baseUrl("http://localhost:" + mockWebServer.getPort())
            .build();

        avgiftOverforingConsumer = new AvgiftOverforingConsumer(webClient);
    }

    @Test
    void hentRepresentantListe() throws IOException, URISyntaxException {
        mockWebServer.enqueue(new MockResponse()
            .setBody(hentMockResponse("mock/representant/representantliste.json"))
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        var response = avgiftOverforingConsumer.hentRepresentantListe();

        assertThat(response)
            .hasSize(3)
            .flatExtracting(AvgiftOverforingRepresentantDto::getId, AvgiftOverforingRepresentantDto::getNavn)
            .containsExactly("99997", "ANDEBYPOSTEN", "99998", "UNIVERSITETET I ANDEBY", "99999", "99DUMMYNAVN");
    }

    @Test
    void hentRepresentant() throws IOException, URISyntaxException {
        mockWebServer.enqueue(new MockResponse()
            .setBody(hentMockResponse("mock/representant/representant.json"))
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        var response = avgiftOverforingConsumer.hentRepresentant("00034");

        assertThat(response.getId()).isEqualTo("99999");
        assertThat(response.getNavn()).isEqualTo("99DUMMYNAVN");
        assertThat(response.getAdresselinjer()).hasSize(3).containsExactly("DUMMYADR1", "DUMMYADR2", "DUMMYADR3");
        assertThat(response.getPostnummer()).isEqualTo("3601");
        assertThat(response.getTelefon()).isNull();
        assertThat(response.getOrgnr()).isNull();
        assertThat(response.getEndretAv()).isEqualTo("RTV");
        assertThat(response.getEndretDato().getYear()).isEqualTo(1997);
        assertThat(response.getEndretDato().getMonthValue()).isEqualTo(3);
        assertThat(response.getEndretDato().getDayOfMonth()).isEqualTo(14);
    }

    private String hentMockResponse(String path) throws URISyntaxException, IOException {
        return new String(
            Files.readAllBytes(
                Paths.get(
                    getClass().getClassLoader().getResource(path).toURI()
                )
            )
        );
    }
}
