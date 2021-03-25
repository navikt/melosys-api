package no.nav.melosys.integrasjon.joark.saf;

import java.io.IOException;

import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SafConsumerImplTest {
    private static final String JOURNALPOST_ID = "1";
    private static final String DOKUMENT_ID = "1";

    private static final String HENT_DOKUMENT_401_RESPONSE = """
        {
          "timestamp": "2021-03-25T08:47:33.594+00:00",
          "status": 401,
          "error": "Unauthorized",
          "message": "no.nav.security.token.support.core.exceptions.JwtTokenMissingException: no valid token found in validation context",
          "path": "/rest/hentdokument/1/1/ARKIV"
        }
        """;

    private static final String HENT_DOKUMENT_404_RESPONSE = """
        {
          "timestamp": "2021-03-25T08:47:33.594+00:00",
          "status": 404,
          "error": "Not Found",
          "message": "Dokumentet tilnyttet journalpostId=1, dokumentInfoId=1, variant=ARKIV ikke funnet.",
          "path": "/rest/hentdokument/1/1/ARKIV"
        }
        """;

    private static MockWebServer mockServer;

    private SafConsumer safConsumer;

    @BeforeAll
    static void setupServer() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
    }


    @BeforeEach
    void setup() {
        safConsumer = new SafConsumerImpl(WebClient.builder().baseUrl("http://localhost:" + mockServer.getPort()).build());
    }

    @Test
    void hentDokument_dokumentFinnes_forventPdf() {
        mockServer.enqueue(
            new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PDF_VALUE)
                .setBody("pdf")
        );

        byte[] pdf = safConsumer.hentDokument(JOURNALPOST_ID, DOKUMENT_ID);
        assertThat(pdf).containsExactly((byte) 'p', (byte) 'd', (byte) 'f');
    }

    @Test
    void hentDokument_dokumentFinnesIkke404Status_kasterIkkeFunnetException() {
        mockServer.enqueue(
            new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(404)
                .setBody(HENT_DOKUMENT_404_RESPONSE)
        );

        assertThatThrownBy(() -> safConsumer.hentDokument(JOURNALPOST_ID, DOKUMENT_ID))
            .hasRootCauseInstanceOf(IkkeFunnetException.class)
            .hasMessageContaining("ikke funnet");
    }

    @Test
    void hentDokument_ikkeAutentisert_kasterSikkerhetsbegrensingException() {
        mockServer.enqueue(
            new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(401)
                .setBody(HENT_DOKUMENT_401_RESPONSE)
        );

        assertThatThrownBy(() -> safConsumer.hentDokument(JOURNALPOST_ID, DOKUMENT_ID))
            .hasRootCauseInstanceOf(SikkerhetsbegrensningException.class)
            .hasMessageContaining("no valid token");
    }
}