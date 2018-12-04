package no.nav.melosys.integrasjon.kodeverk.impl;

import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@TestPropertySource("classpath:test.properties")
public class KodeverkConsumerTestIT {

    @Value("${KodeverkAPI_v1.url}")
    String endpointUrl;

    @Test
    public void isAlive() {
        String url = endpointUrl.replaceAll("api$", "internal/isReady");
        Response response = ClientBuilder.newClient()
            .target(url)
            .request().get();

        assertNotNull(response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

}
