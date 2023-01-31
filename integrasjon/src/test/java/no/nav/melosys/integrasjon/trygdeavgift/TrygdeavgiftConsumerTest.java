package no.nav.melosys.integrasjon.trygdeavgift;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

import no.nav.melosys.domain.kodeverk.Folketrygdloven_kap2_bestemmelser;
import no.nav.melosys.domain.kodeverk.Saerligeavgiftsgrupper;
import no.nav.melosys.domain.kodeverk.Trygdedekninger;
import no.nav.melosys.integrasjon.trygdeavgift.dto.MelosysTrygdeavgfitBeregningDto;
import no.nav.melosys.integrasjon.trygdeavgift.dto.TrygdeavgiftDto;
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

        var a = lagBeregningsgrunnlagDto();
        List<TrygdeavgiftDto> response = trygdeavgiftConsumer.beregnTrygdeavgift(a);
        assertThat(response.get(0))
            .extracting(TrygdeavgiftDto::getAvgiftskode, TrygdeavgiftDto::getAvgiftssats, TrygdeavgiftDto::getMaanedsavgift)
            .containsExactly("B2R", new BigDecimal("21.8"), new BigDecimal(21800));
    }

    private MelosysTrygdeavgfitBeregningDto lagBeregningsgrunnlagDto() {
        return new MelosysTrygdeavgfitBeregningDto(
            Boolean.FALSE,
            Boolean.FALSE,
            Trygdedekninger.HELSEDEL,
            Folketrygdloven_kap2_bestemmelser.FTRL_KAP2_2_8,
            123321,
            Saerligeavgiftsgrupper.ARBEIDSTAKER_MALAYSIA,
            LocalDate.of(2022, 05, 12),
            LocalDate.of(2023, 11, 03));
    }

    private String hentMockRespons() throws URISyntaxException, IOException {
        return new String(
            Files.readAllBytes(
                Paths.get(
                    getClass().getClassLoader().getResource("mock/trygdedekning/trygdeavgift.json").toURI()
                )
            )
        );
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockServer.shutdown();
    }
}
