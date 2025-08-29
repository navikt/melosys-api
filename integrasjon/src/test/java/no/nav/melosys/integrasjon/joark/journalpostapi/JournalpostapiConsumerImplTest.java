package no.nav.melosys.integrasjon.joark.journalpostapi;

import no.nav.melosys.integrasjon.joark.journalpostapi.dto.FerdigstillJournalpostRequest;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OppdaterJournalpostRequest;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import java.time.LocalDate;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JournalpostapiConsumerImplTest {

    private JournalpostapiConsumer journalpostapiConsumer;
    private WireMockServer wireMockServer;

    @BeforeAll
    public void initialSetup() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();

        WebClient webClient = WebClient.builder()
            .baseUrl(wireMockServer.baseUrl())
            .build();

        journalpostapiConsumer = new JournalpostapiConsumer(webClient);
    }

    @BeforeEach
    public void setup() {
        wireMockServer.resetAll();

        // Stub all requests to return success
        stubFor(any(urlMatching(".*"))
            .willReturn(aResponse()
                .withStatus(200)
                .withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .withBody("{}")));
    }

    @Test
    void opprettJournalpost_verifiserUrl() {
        OpprettJournalpostRequest req = new OpprettJournalpostRequest.
            OpprettJournalpostRequestBuilder()
            .journalpostType(OpprettJournalpostRequest.JournalpostType.INNGAAENDE)
            .build();

        journalpostapiConsumer.opprettJournalpost(req, true);

        wireMockServer.verify(
            postRequestedFor(urlPathEqualTo("/journalpost"))
                .withQueryParam("forsoekFerdigstill", equalTo("true"))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON_VALUE))
        );
    }

    @Test
    void oppdaterJournalpost_verifiserUrl() {
        final String journalpostID = "123123";

        journalpostapiConsumer.oppdaterJournalpost(new OppdaterJournalpostRequest.Builder().build(), journalpostID);

        wireMockServer.verify(
            putRequestedFor(urlPathEqualTo("/journalpost/" + journalpostID))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON_VALUE))
        );
    }

    @Test
    void leggTilLogiskVedlegg_verifiserUrl() {
        final String dokumentInfoId = "532";

        journalpostapiConsumer.leggTilLogiskVedlegg(dokumentInfoId, "titteltittei");

        wireMockServer.verify(
            postRequestedFor(urlPathEqualTo("/dokumentInfo/" + dokumentInfoId + "/logiskVedlegg/"))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON_VALUE))
        );
    }

    @Test
    void fjernLogiskeVedlegg_verifiserUrl() {
        final String dokumentInfoID = "124j";
        final String logiskVedleggID = "3j2io";

        journalpostapiConsumer.fjernLogiskeVedlegg(dokumentInfoID, logiskVedleggID);

        wireMockServer.verify(
            deleteRequestedFor(urlPathEqualTo("/dokumentInfo/" + dokumentInfoID + "/logiskVedlegg/" + logiskVedleggID))
        );
    }

    @Test
    void ferdigstillJournalpost_verifiserUrl() {
        final String journalpostID = "54325";

        journalpostapiConsumer.ferdigstillJournalpost(new FerdigstillJournalpostRequest(), journalpostID);

        wireMockServer.verify(
            patchRequestedFor(urlPathEqualTo("/journalpost/" + journalpostID + "/ferdigstill"))
                .withHeader(HttpHeaders.CONTENT_TYPE, equalTo(MediaType.APPLICATION_JSON_VALUE))
        );
    }

    @Test
    void opprettJournalpost_verifiserDatoMottatt() {
        final String datoMottatt = "1970-01-01";

        OpprettJournalpostRequest req = new OpprettJournalpostRequest.
            OpprettJournalpostRequestBuilder()
            .datoMottatt(LocalDate.parse(datoMottatt))
            .journalpostType(OpprettJournalpostRequest.JournalpostType.INNGAAENDE)
            .build();

        journalpostapiConsumer.opprettJournalpost(req, true);

        wireMockServer.verify(
            postRequestedFor(urlPathEqualTo("/journalpost"))
                .withRequestBody(matchingJsonPath("$.datoMottatt", equalTo(datoMottatt)))
        );
    }
}
