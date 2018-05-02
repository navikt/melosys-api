package no.nav.melosys.integrasjon.gsak.sakapi;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
public class SakApiConsumerTestIT {

    @Autowired
    SakApiConsumerConfig config;

    @Test
    public void isAlive() {
        Response response = ClientBuilder.newClient()
                .target(config.getEndpointUrl())
                .path("internal/alive")
                .request().get();

        assertNotNull(response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

}
