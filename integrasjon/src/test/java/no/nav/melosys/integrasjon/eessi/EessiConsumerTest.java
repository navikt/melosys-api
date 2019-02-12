package no.nav.melosys.integrasjon.eessi;

import java.util.Map;

import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withBadRequest;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(MockitoJUnitRunner.class)
public class EessiConsumerTest {

    @Spy
    private RestTemplate restTemplate;
    private EessiConsumer eessiConsumer;
    private MockRestServiceServer server;

    private SedDataDto sedDataDto;

    @Before
    public void setup() {
        server = MockRestServiceServer.createServer(restTemplate);
        eessiConsumer = new EessiConsumerImpl(restTemplate);
        sedDataDto = new SedDataDto();
    }

    @Test
    public void opprettOgSend_forventMap() throws Exception {
        server.expect(requestTo("/sed/createAndSend"))
            .andRespond(withSuccess("{\"rinaCaseId\":\"123132\"}", MediaType.APPLICATION_JSON));
        Map<String, String> resultat = eessiConsumer.opprettOgSendSed(sedDataDto);
        assertThat(resultat.get("rinaCaseId"), is("123132"));
    }

    @Test(expected = MelosysException.class)
    public void opprettOgSend_forventException() throws Exception {
        server.expect(requestTo("/sed/createAndSend"))
            .andRespond(withBadRequest());
        eessiConsumer.opprettOgSendSed(sedDataDto);
    }

}