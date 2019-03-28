package no.nav.melosys.integrasjon.joark.journalfoerinngaaende;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.dok.tjenester.journalfoerinngaaende.*;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

public class JournalfoerInngaaendeConsumerTest {

    private JournalfoerInngaaendeConsumer journalfoerInngaaendeConsumer;

    private RestTemplate restTemplate = new RestTemplate();
    private MockRestServiceServer server;

    private ObjectMapper objectMapper = new ObjectMapper();

    private final String JOURNALPOST_ID = "j123";
    private final String DOKUMENT_ID = "d123";

    @Before
    public void setup() throws Exception {
        journalfoerInngaaendeConsumer = new JournalfoerInngaaendeConsumer(restTemplate);
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void hentJournalpost_verifiserUrlOgJson() throws Exception {
        String url = String.format("/journalposter/%s", JOURNALPOST_ID);

        server.expect(requestTo(url))
            .andRespond(withSuccess(objectMapper.writeValueAsString(new GetJournalpostResponse()), APPLICATION_JSON));

        journalfoerInngaaendeConsumer.hentJournalpost(JOURNALPOST_ID);
    }

    @Test
    public void oppdaterJournalpost_verifiserUrlOgJson() throws Exception {
        String url = String.format("/journalposter/%s", JOURNALPOST_ID);

        server.expect(requestTo(url))
            .andRespond(withSuccess(objectMapper.writeValueAsString(new PutJournalpostResponse()), APPLICATION_JSON));

        journalfoerInngaaendeConsumer.oppdaterJournalpost(new PutJournalpostRequest(), JOURNALPOST_ID);
    }

    @Test
    public void oppdaterDokument_verifiserUrlOgJson() throws Exception {
        String url = String.format("/journalposter/%s/dokumenter/%s", JOURNALPOST_ID, DOKUMENT_ID);

        server.expect(requestTo(url))
            .andRespond(withSuccess(objectMapper.writeValueAsString(new PutDokumentResponse()), APPLICATION_JSON));

        journalfoerInngaaendeConsumer.oppdaterDokument(new PutDokumentRequest(), JOURNALPOST_ID, DOKUMENT_ID);
    }

    @Test
    public void leggTilLogiskVedlegg_verifiserUrlOgJson() throws Exception {
        String url = String.format("/journalposter/%s/dokumenter/%s/logiskeVedlegg", JOURNALPOST_ID, DOKUMENT_ID);

        server.expect(requestTo(url))
            .andRespond(withSuccess(objectMapper.writeValueAsString(new PostLogiskVedleggResponse()), APPLICATION_JSON));

        journalfoerInngaaendeConsumer.leggTilLogiskVedlegg(new PostLogiskVedleggRequest(), JOURNALPOST_ID, DOKUMENT_ID);
    }
}