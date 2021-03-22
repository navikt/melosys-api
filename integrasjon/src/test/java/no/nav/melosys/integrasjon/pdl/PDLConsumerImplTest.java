package no.nav.melosys.integrasjon.pdl;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Objects;

import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.pdl.dto.identer.Ident;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import static no.nav.melosys.integrasjon.pdl.dto.identer.IdentGruppe.AKTORID;
import static no.nav.melosys.integrasjon.pdl.dto.identer.IdentGruppe.FOLKEREGISTERIDENT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class PDLConsumerImplTest {
    private static MockWebServer mockServer;

    private PDLConsumer pdlConsumer;

    @BeforeAll
    static void setupServer() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
    }

    @BeforeEach
    public void setup() {
        pdlConsumer = new PDLConsumerImpl(
            WebClient.builder().baseUrl(String.format("http://localhost:%s", mockServer.getPort()))
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE).build());
    }

    @Test
    void hentIdenter_medIdent_mottarOgMapperResponseUtenFeil() throws IkkeFunnetException, IntegrasjonException {
        mockServer.enqueue(
            new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(lastFil("mock/pdl/hentIdenter.json"))
        );

        assertThat(pdlConsumer.hentIdenter("123").identer()).containsExactly(
            new Ident("99026522600", FOLKEREGISTERIDENT), new Ident("9834873315250", AKTORID));
    }

    @Test
    void hentIdenter_feilFraPDL_kasterFeil() {
        mockServer.enqueue(
            new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(lastFil("mock/pdl/feil.json"))
        );

        assertThatExceptionOfType(IntegrasjonException.class).isThrownBy(
            () -> pdlConsumer.hentIdenter("123").identer())
            .withMessageContaining("My error message");
    }

    private String lastFil(String filnavn) {
        try {
            return Files.readString(Paths.get(
                Objects.requireNonNull(getClass().getClassLoader().getResource(filnavn)).toURI()
            ));
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
}
