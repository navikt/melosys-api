package no.nav.melosys.integrasjon.joark.journalfoerinngaaende;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.dok.tjenester.journalfoerinngaaende.GetJournalpostResponse;
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

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setup() {
        journalfoerInngaaendeConsumer = new JournalfoerInngaaendeConsumer(restTemplate);
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void hentJournalpost_verifiserUrlOgJson() throws Exception {
        String journalpostID = "j123";
        server.expect(requestTo(String.format("/journalposter/%s", journalpostID)))
            .andRespond(withSuccess(objectMapper.writeValueAsString(new GetJournalpostResponse()), APPLICATION_JSON));

        journalfoerInngaaendeConsumer.hentJournalpost(journalpostID);
    }
}