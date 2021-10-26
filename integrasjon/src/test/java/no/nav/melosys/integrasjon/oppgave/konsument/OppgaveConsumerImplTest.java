package no.nav.melosys.integrasjon.oppgave.konsument;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveDto;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveSearchRequest;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OpprettOppgaveDto;
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

class OppgaveConsumerImplTest {

    private static MockWebServer mockServer;

    private static final String OPPGAVE_GET_JSON_PATH = "mock/oppgave/oppgave_get.json";
    private static final String OPPGAVELIST_GET_JSON_PATH = "mock/oppgave/hentOppgaveListe_get.json";
    private static final String OPPGAVE_FEILMELDING_JSON_PATH = "mock/oppgave/feil.json";
    private OppgaveConsumer oppgaveConsumer;

    @BeforeAll
    static void setupServer() throws IOException {
        mockServer = new MockWebServer();
        mockServer.start();
    }

    @BeforeEach
    void setup() {
        oppgaveConsumer = new OppgaveConsumerImpl(WebClient.builder().baseUrl("http://localhost:" + mockServer.getPort()).build());
    }

    @Test
    void hentOppgave_oppgaveFinnes_verifiserMapping() throws Exception {
        mockServer.enqueue(
            new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(testFil(OPPGAVE_GET_JSON_PATH))
        );

        assertThat(oppgaveConsumer.hentOppgave("1")).extracting(
            OppgaveDto::getId,
            OppgaveDto::getTildeltEnhetsnr,
            OppgaveDto::getJournalpostId,
            OppgaveDto::getBehandlesAvApplikasjon,
            OppgaveDto::getSaksreferanse,
            OppgaveDto::getAktørId,
            OppgaveDto::getTilordnetRessurs,
            OppgaveDto::getBeskrivelse,
            OppgaveDto::getTema,
            OppgaveDto::getBehandlingstema,
            OppgaveDto::getOppgavetype,
            OppgaveDto::getVersjon,
            OppgaveDto::getPrioritet,
            OppgaveDto::getStatus,
            OppgaveDto::getFristFerdigstillelse,
            OppgaveDto::getAktivDato,
            OppgaveDto::getOpprettetTidspunkt,
            OppgaveDto::getBehandlingstype
        ).containsExactly(
            "11519",
            "4530",
            "439654251",
            "FS38",
            "MEL-301",
            "1332607802528",
            "Z990757",
            " ",
            "MED",
            "ab0390",
            "BEH_SED",
            3,
            "NORM",
            "AAPNET",
            LocalDate.parse("2019-12-26"),
            LocalDate.parse("2019-10-03"),
            ZonedDateTime.parse("2019-10-03T10:27:26.566Z[UTC]"),
            null
        );
    }

    @Test
    void hentOppgave_oppgaveFinnesIkke404Status_kasterIkkeFunnetException() throws Exception {
        mockServer.enqueue(
            new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(404)
                .setBody(testFil(OPPGAVE_FEILMELDING_JSON_PATH))
        );

        assertThatThrownBy(() -> oppgaveConsumer.hentOppgave("1"))
            .isInstanceOf(IkkeFunnetException.class)
            .hasMessageContaining("Fant ingen oppgave");
    }

    @Test
    void hentOppgaveListe_mottarToOppgaver_verifiserMapping() throws Exception {
        mockServer.enqueue(
            new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(testFil(OPPGAVELIST_GET_JSON_PATH))
        );

        assertThat(oppgaveConsumer.hentOppgaveListe(
            new OppgaveSearchRequest.Builder("123")
                .medOppgaveTyper(new String[]{"BEH_SED", "BEH_SAK"})
                .medAktørId("123")
                .medBehandlingstema("ab2344")
                .medBehandlingsType("ba432?")
                .medBehandlesAvApplikasjon("FS38")
                .medTema(new String[]{"MED", "UFM"})
                .medStatusKategori("AAPEN")
                .build()
        )).hasSize(2)
            .flatExtracting(OppgaveDto::getSaksreferanse)
            .containsExactlyInAnyOrder("MEL-301", "MEL-513");
    }

    @Test
    void oppdaterOppgave_oppgaveOppdateres_verifiserMapping() throws Exception {
        mockServer.enqueue(
            new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(testFil(OPPGAVE_GET_JSON_PATH))
        );

        assertThat(oppgaveConsumer.oppdaterOppgave(new OppgaveDto()))
            .extracting(OppgaveDto::getId)
            .isEqualTo("11519");
    }

    @Test
    void opprettOppgave_oppgaveOpprettes_verifiserMapping() throws Exception {
        mockServer.enqueue(
            new MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(testFil(OPPGAVE_GET_JSON_PATH))
        );

        String oppgaveID = oppgaveConsumer.opprettOppgave(new OpprettOppgaveDto());
        assertThat(oppgaveID).isEqualTo("11519");
    }

    private String testFil(String path) throws IOException, URISyntaxException {
        return new String(
            Files.readAllBytes(
                Paths.get(
                    Optional.ofNullable(getClass().getClassLoader().getResource(path))
                        .orElseThrow(() -> new NoSuchElementException("Ingen fil " + path))
                        .toURI()
                )
            )
        );
    }
}
