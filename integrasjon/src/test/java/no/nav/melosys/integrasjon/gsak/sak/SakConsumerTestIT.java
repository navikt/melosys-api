package no.nav.melosys.integrasjon.gsak.sak;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
public class SakConsumerTestIT {

    @Value("${SakAPI_v1.url}")
    String endpointUrl;

    @Test
    public void isAlive() {
        Response response = ClientBuilder.newClient()
                .target(endpointUrl)
                .path("internal/alive")
                .request().get();

        assertNotNull(response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

}
