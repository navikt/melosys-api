package no.nav.melosys.integrasjon.trygdeavgift;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import no.nav.melosys.integrasjon.trygdeavgift.dto.Penger;
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftBeregningsgrunnlag;
import no.nav.melosys.integrasjon.trygdeavgift.dto.Trygdeavgiftsperiode;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

class TrygdeavgiftConsumerTest {

    private static MockWebServer mockServer;

    private TrygdeavgiftConsumer trygdeavgiftConsumer;

    private final String url = format("http://localhost:%s", mockServer.getPort());

    @BeforeAll
    static void setupServer() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
    }

    @BeforeEach
    void setup() {
        trygdeavgiftConsumer = new TrygdeavgiftConsumer(url);
    }

    @Test
    void beregnTrygdeavgift() throws IOException, URISyntaxException {
        mockServer.enqueue(new MockResponse()
            .setBody(hentMockRespons())
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        );

        var a = lagTrygdeavgiftBeregningsgrunnlag();
        List<Trygdeavgiftsperiode> response = trygdeavgiftConsumer.beregnTrygdeavgift(a);
        assertThat(response.get(0))
            .extracting(Trygdeavgiftsperiode::getSats, Trygdeavgiftsperiode::getAvgift)
            .containsExactly(new BigDecimal("21.8"), new Penger(BigInteger.valueOf(21800)));
    }

    private TrygdeavgiftBeregningsgrunnlag lagTrygdeavgiftBeregningsgrunnlag() {
        return new TrygdeavgiftBeregningsgrunnlag(Collections.emptySet(), Collections.emptySet(), Collections.emptyList());
    }

    private String hentMockRespons() throws URISyntaxException, IOException {
        return new String(
            Files.readAllBytes(
                Paths.get(
                    getClass().getClassLoader().getResource("mock/trygdeavgift/trygdeavgift.json").toURI()
                )
            )
        );
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockServer.shutdown();
    }
}
