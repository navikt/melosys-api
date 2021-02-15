package no.nav.melosys.integrasjon.avgiftoverforing;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static java.lang.String.format;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class AvgiftOverforingConsumerTest {

    private static MockWebServer mockWebServer;

    private AvgiftOverforingConsumer avgiftOverforingConsumer;

    private final String url = format("http://localhost:%s", mockWebServer.getPort());

    @BeforeAll
    static void setupServer() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @BeforeEach
    void setup() {
        avgiftOverforingConsumer = new AvgiftOverforingConsumer(url);
    }

    @Test
    void hentRepresentantListe() throws IOException, URISyntaxException {
        mockWebServer.enqueue(new MockResponse()
            .setBody(hentMockResponse("mock/representant/representantliste.json"))
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        var response = avgiftOverforingConsumer.hentRepresentantListe();

        assertThat(response.length).isEqualTo(3);
        assertThat(response[0].getId()).isEqualTo("99997");
        assertThat(response[0].getNavn()).isEqualTo("ANDEBYPOSTEN");
        assertThat(response[1].getId()).isEqualTo("99998");
        assertThat(response[1].getNavn()).isEqualTo("UNIVERSITETET I ANDEBY");
        assertThat(response[2].getId()).isEqualTo("99999");
        assertThat(response[2].getNavn()).isEqualTo("99DUMMYNAVN");
    }

    @Test
    void hentRepresentant() throws IOException, URISyntaxException {
        mockWebServer.enqueue(new MockResponse()
            .setBody(hentMockResponse("mock/representant/representant.json"))
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE));

        var response = avgiftOverforingConsumer.hentRepresentant("00034");

        assertThat(response.getId()).isEqualTo("99999");
        assertThat(response.getNavn()).isEqualTo("99DUMMYNAVN");
        assertThat(response.getAdresselinjer().size()).isEqualTo(3);
        assertThat(response.getAdresselinjer().get(0)).isEqualTo("DUMMYADR1");
        assertThat(response.getAdresselinjer().get(1)).isEqualTo("DUMMYADR2");
        assertThat(response.getAdresselinjer().get(2)).isEqualTo("DUMMYADR3");
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
