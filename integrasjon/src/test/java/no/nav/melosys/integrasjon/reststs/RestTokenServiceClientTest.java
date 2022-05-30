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
import org.springframework.web.client.RestTemplate;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RestTokenServiceClientTest {

    private RestTokenServiceClientClient restTokenServiceClient;

    @Mock
    private RestTemplate restTemplate;

    @BeforeEach
    public void setUp() {
        restTokenServiceClient = spy(new RestTokenServiceClientClient(restTemplate));

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

        Map<String, Object> body= Maps.newHashMap();
        body.put("access_token","123abc");
        body.put("expires_in", 30L);

        ResponseEntity<Map> responseEntity = new ResponseEntity<>(body, HttpStatus.OK);

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                .thenReturn(responseEntity);

        String token = restTokenServiceClient.collectToken();
        verify(restTokenServiceClient, times(1)).basicAuth();
        assertNotNull(token);
        assertFalse(token.isEmpty());

        body.put("access_token", "cba321");
        body.put("expires_in", 3600L);

        String secondToken = restTokenServiceClient.collectToken();
        verify(restTokenServiceClient, times(2)).basicAuth();
        assertNotEquals(token, secondToken);

        body.put("access_token", "abccba");

        String thirdToken = restTokenServiceClient.collectToken();
        verify(restTokenServiceClient, times(2)).basicAuth();
        assertEquals(secondToken, thirdToken);
    }
}
