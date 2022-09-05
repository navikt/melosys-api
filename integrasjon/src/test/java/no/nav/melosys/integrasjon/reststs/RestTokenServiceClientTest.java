package no.nav.melosys.integrasjon.reststs;

import java.util.Map;

import com.google.common.collect.Maps;
import no.nav.melosys.integrasjon.felles.EnvironmentHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.reactive.function.client.WebClient;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RestTokenServiceClientTest {

    private RestTokenServiceClient restTokenServiceClient;

    @Mock
    private WebClient webClient;

    @BeforeEach
    public void setUp() {
        restTokenServiceClient = spy(new RestTokenServiceClient(webClient));

        // Setter environment som "singleton"
        MockEnvironment environment = spy(new MockEnvironment());
        environment.setProperty("systemuser.username", "test");
        environment.setProperty("systemuser.password", "test");
        new EnvironmentHandler(environment);
    }

    //Tester at token blir hentet på nytt ved kort expires_in, og ikke ved lengre expires_in
    @Test
    @SuppressWarnings("unchecked")
    public void testCollectToken() throws Exception{

    }
}
