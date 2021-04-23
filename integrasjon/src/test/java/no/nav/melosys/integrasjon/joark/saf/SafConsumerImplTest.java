package no.nav.melosys.integrasjon.joark.saf;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.felles.graphql.GraphQLResponse;
import no.nav.melosys.integrasjon.joark.saf.dto.journalpost.*;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.jetbrains.annotations.NotNull;
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

    private static final String HENT_JOURNALPOST_401_RESPONSE = """
        {
          "timestamp": "2021-03-25T08:47:33.594+00:00",
          "status": 401,
          "error": "Unauthorized",
          "message": "no.nav.security.token.support.core.exceptions.JwtTokenMissingException: no valid token found in validation context",
          "path": "/graphql"
        }
        """;

    private static final String HENT_JOURNALPOST_IKKE_FUNNET_RESPONSE = """
        {
            "errors": [
                {
                    "message": "Journalpost med journalpostId=1 ikke funnet.",
                    "locations": [],
                    "extensions": {
                        "classification": "DataFetchingException"
                    }
                }
            ],
            "data": {
                "query": null
            }
        }
        """;

    private static final ObjectWriter OBJECT_WRITER = new ObjectMapper().writer();
    private static MockWebServer mockServer;

    private SafConsumer safConsumer;

    @BeforeEach
    void setup() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();

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

    @Test
    void hentJournalpost_journalpostFinnesIkke_kasterIntegrasjonException() {
        mockServer.enqueue(
            new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(200)
                .setBody(HENT_JOURNALPOST_IKKE_FUNNET_RESPONSE)
        );

        assertThatThrownBy(() -> safConsumer.hentJournalpost(JOURNALPOST_ID))
            .isInstanceOf(IntegrasjonException.class)
            .hasMessageContaining("ikke funnet");
    }

    @Test
    void hentJournalpost_ikkeAutentisert_kasterSikkerhetsbegrensingException() {
        mockServer.enqueue(
            new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(401)
                .setBody(HENT_JOURNALPOST_401_RESPONSE)
        );

        assertThatThrownBy(() -> safConsumer.hentJournalpost(JOURNALPOST_ID))
            .hasRootCauseInstanceOf(SikkerhetsbegrensningException.class)
            .hasMessageContaining("no valid token");
    }

    @Test
    void hentDokumentoversikt_ingenPaginering_forventAntallJournalposterOgKall() {
        mockServer.enqueue(responseAv("peker", false));

        Collection<Journalpost> journalposter = safConsumer.hentDokumentoversikt("MEL-1");
        assertThat(journalposter).hasSize(10);
        assertThat(mockServer.getRequestCount()).isEqualTo(1);
    }

    @Test
    void hentDokumentoversikt_medPaginering_forventAntallJournalposterOgKall() {
        mockServer.setDispatcher(
            new Dispatcher() {
                @NotNull
                @Override
                public MockResponse dispatch(@NotNull RecordedRequest req) {
                    if (harPeker(req, "p1")) {
                        return responseAv("p2", true);
                    }
                    if (harPeker(req, "p2")) {
                        return responseAv("p3", true);
                    }
                    if (harPeker(req, "p3")) {
                        return responseAv("p4", false);
                    }
                    return responseAv("p1", true);
                }
            }
        );

        Collection<Journalpost> journalposter = safConsumer.hentDokumentoversikt("MEL-1");
        assertThat(journalposter).hasSize(40);
        assertThat(mockServer.getRequestCount()).isEqualTo(4);
    }

    private static boolean harPeker(RecordedRequest req, String peker) {
        try {
            return req.getBody().peek().readUtf8().contains(peker);
        } catch (IOException e) {
            throw new RuntimeException("Kunne ikke lese request");
        }
    }

    private static MockResponse responseAv(String nestePeker, boolean finnesNeste) {
        try {
            return new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(200)
                .setBody(OBJECT_WRITER.writeValueAsString(lagHentDokumentoversiktResponse(nestePeker, finnesNeste)));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Kunne ikke serialisere response");
        }
    }

    private static GraphQLResponse<HentDokumentoversiktResponseWrapper> lagHentDokumentoversiktResponse(String peker, boolean finnesNeste) {
        return new GraphQLResponse<>(
            new HentDokumentoversiktResponseWrapper(
                new HentDokumentoversiktResponse(
                    lagJournalposter(10),
                    new SideInfo(peker, finnesNeste))
            ), Collections.emptyList()
        );
    }

    private static List<Journalpost> lagJournalposter(int antall) {
        return Stream.generate(SafConsumerImplTest::lagJournalpost).limit(antall).collect(Collectors.toList());
    }

    private static Journalpost lagJournalpost() {
        return new Journalpost(
            "id",
            "tittel",
            Journalstatus.JOURNALFOERT,
            "MED",
            Journalposttype.I,
            new Sak("123"),
            new Bruker("123", Brukertype.FNR),
            new AvsenderMottaker("123", AvsenderMottakerType.FNR, "navn"),
            "SED",
            Collections.emptyList(),
            Collections.emptyList()
        );
    }
}
