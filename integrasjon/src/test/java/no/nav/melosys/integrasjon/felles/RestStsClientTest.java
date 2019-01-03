package no.nav.melosys.integrasjon.felles;

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RestStsClientTest {

    private RestStsClient restSTSClient;

    @Mock
    private RestTemplate restTemplate;

    @Before
    public void setUp() {
        restSTSClient = spy(new RestStsClient(restTemplate));

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

        when(restTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), any(Class.class)))
                .thenReturn(responseEntity);

        String token = restSTSClient.collectToken();
        verify(restSTSClient, times(1)).basicAuth();
        assertNotNull(token);
        assertFalse(token.isEmpty());

        body.put("access_token", "cba321");
        body.put("expires_in", 3600L);

        String secondToken = restSTSClient.collectToken();
        verify(restSTSClient, times(2)).basicAuth();
        assertNotEquals(token, secondToken);

        body.put("access_token", "abccba");

        String thirdToken = restSTSClient.collectToken();
        verify(restSTSClient, times(2)).basicAuth();
        assertEquals(secondToken, thirdToken);
    }
}
