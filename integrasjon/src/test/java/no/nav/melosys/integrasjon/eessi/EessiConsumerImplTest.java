package no.nav.melosys.integrasjon.eessi;

import no.nav.melosys.domain.dokument.sed.SedType;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.eessi.dto.SedDataDto;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@RunWith(MockitoJUnitRunner.class)
public class EessiConsumerImplTest {
    private EessiConsumer eessiConsumer;
    private MockRestServiceServer server;

    @Before
    public void setup() {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        eessiConsumer = new EessiConsumerImpl(restTemplate);
        server = MockRestServiceServer.createServer(restTemplate);
    }

    @Test
    public void genererSedForhåndsvisning() throws MelosysException {
        final byte[] PDF = "pdf".getBytes();
        server.expect(requestTo("/sed/A001/pdf"))
            .andRespond(withSuccess(PDF, MediaType.APPLICATION_PDF));

        byte[] pdf = eessiConsumer.genererSedForhåndsvisning(new SedDataDto(), SedType.A001);
        assertThat(pdf).isEqualTo(PDF);
    }
}