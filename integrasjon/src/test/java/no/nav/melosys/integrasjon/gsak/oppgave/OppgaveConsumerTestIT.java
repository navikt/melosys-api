package no.nav.melosys.integrasjon.gsak.oppgave;

import java.security.NoSuchAlgorithmException;
import javax.net.ssl.SSLContext;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit4.SpringRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SuppressWarnings("resource")
public class OppgaveConsumerTestIT {

    @Value("${OppgaveAPI_v1.url}")
    private String endpointUrl;

    @Test
    public void isAlive() throws NoSuchAlgorithmException {
        SSLContext sslContext = SSLContext.getDefault();
        Response response = ClientBuilder
            .newBuilder()
            .sslContext(sslContext)
            .build()
            .target(endpointUrl)
            .path("internal/alive")
            .request()
            .get();

        assertNotNull(response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

}
