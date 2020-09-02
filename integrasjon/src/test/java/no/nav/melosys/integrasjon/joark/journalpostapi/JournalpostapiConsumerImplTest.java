package no.nav.melosys.integrasjon.joark.journalpostapi;

import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.FerdigstillJournalpostRequest;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OppdaterJournalpostRequest;
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostRequest;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class JournalpostapiConsumerImplTest {

    private JournalpostapiConsumer journalpostapiConsumer;
    private RestTemplate restTemplate = new RestTemplate();

    private MockRestServiceServer server;

    @Before
    public void setup() {
        server = MockRestServiceServer.createServer(restTemplate);
        journalpostapiConsumer = new JournalpostapiConsumerImpl(restTemplate);
    }

    @Test
    public void opprettJournalpost_verifiserUrl() {
        server.expect(requestTo("/journalpost?forsoekFerdigstill=true"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess());

        OpprettJournalpostRequest req = new OpprettJournalpostRequest.
            OpprettJournalpostRequestBuilder()
            .journalpostType(OpprettJournalpostRequest.JournalpostType.INNGAAENDE)
            .build();

        journalpostapiConsumer.opprettJournalpost(req, true);
    }

    @Test
    public void oppdaterJournalpost_verifiserUrl() throws SikkerhetsbegrensningException, IntegrasjonException {
        final String journalpostID = "123123";
        server.expect(requestTo("/journalpost/" + journalpostID))
            .andExpect(method(HttpMethod.PUT))
            .andRespond(withSuccess());

        journalpostapiConsumer.oppdaterJournalpost(new OppdaterJournalpostRequest.Builder().build(), journalpostID);
    }

    @Test
    public void leggTilLogiskVedlegg_verifiserUrl() throws SikkerhetsbegrensningException, IntegrasjonException {
        final String dokumentInfoId = "532";
        server.expect(requestTo("/dokumentInfo/" + dokumentInfoId + "/logiskVedlegg/"))
            .andExpect(method(HttpMethod.POST))
            .andRespond(withSuccess());

        journalpostapiConsumer.leggTilLogiskVedlegg(dokumentInfoId, "titteltittei");
    }

    @Test
    public void fjernLogiskeVedlegg_verifiserUrl() throws SikkerhetsbegrensningException, IntegrasjonException {
        final String dokumentInfoID = "124j";
        final String logiskVedleggID = "3j2io";
        server.expect(requestTo("/dokumentInfo/" + dokumentInfoID + "/logiskVedlegg/" + logiskVedleggID))
            .andExpect(method(HttpMethod.DELETE))
            .andRespond(withSuccess());

        journalpostapiConsumer.fjernLogiskeVedlegg(dokumentInfoID, logiskVedleggID);
    }

    @Test
    public void ferdigstillJournalpost_verifiserUrl() throws SikkerhetsbegrensningException, IntegrasjonException {
        final String journalpostID = "54325";
        server.expect(requestTo("/journalpost/" + journalpostID + "/ferdigstill"))
            .andExpect(method(HttpMethod.PATCH))
            .andRespond(withSuccess());

        journalpostapiConsumer.ferdigstillJournalpost(new FerdigstillJournalpostRequest(), journalpostID);
    }
}