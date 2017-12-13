package no.nav.melosys.sikkerhet.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(org.mockito.junit.MockitoJUnitRunner.class)
public class OidcRefresherTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Captor
    private ArgumentCaptor<HttpEntity<String>> captor;

    @Mock
    private RestTemplate restTemplateMock;

    private MockEnvironment env;

    private OidcRefresher refresher;

    @Before
    public void before() {
        env = new MockEnvironment();
        env.setProperty("OpenIdConnect.issoHost", "tokenEndpoint");
        env.setProperty("OpenIdConnect.username", "melosys");
        env.setProperty("OpenIdConnect.password", "secret");

        refresher = new OidcRefresher(restTemplateMock, env);

        JsonNode originalResponse = mock(JsonNode.class);
        ResponseEntity<JsonNode> response = new ResponseEntity<>(originalResponse, HttpStatus.OK);
        when(restTemplateMock.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(JsonNode.class))).thenReturn(response);
        JsonNode idTokenNode = mock(JsonNode.class);
        when(idTokenNode.asText()).thenReturn("token");
        when(originalResponse.get("id_token")).thenReturn(idTokenNode);
    }

    @Test
    public void callsRestTemplateWithAuthorizationHeader() {
        refresher.refreshOidcToken("OIDC");

        verify(restTemplateMock).exchange(eq("tokenEndpoint/access_token"), eq(HttpMethod.POST), captor.capture(), eq(JsonNode.class));
        HttpEntity<String> entity = captor.getValue();
        assertThat(entity.getHeaders().get("Authorization"), hasSize(1));
        assertThat(entity.getHeaders().get("Authorization").get(0), is("Basic " + Base64.getEncoder().encodeToString("melosys:secret".getBytes())));
    }

    @Test
    public void callsRestTemplateWithContentTypeHeader() {
        refresher.refreshOidcToken("OIDC");

        verify(restTemplateMock).exchange(eq("tokenEndpoint/access_token"), eq(HttpMethod.POST), captor.capture(), eq(JsonNode.class));
        HttpEntity<String> entity = captor.getValue();
        assertThat(entity.getHeaders().get("Content-type"), hasSize(1));
        assertThat(entity.getHeaders().get("Content-type").get(0), is("application/x-www-form-urlencoded"));
    }

    @Test
    public void callsRestTemplateWithExpectedBody() {
        refresher.refreshOidcToken("OIDC");

        verify(restTemplateMock).exchange(eq("tokenEndpoint/access_token"), eq(HttpMethod.POST), captor.capture(), eq(JsonNode.class));
        HttpEntity<String> entity = captor.getValue();
        assertThat(entity.getBody(), is(OidcRefresher.POST_BODY + "OIDC"));
    }

    @Test
    public void returnsIdToken() {
        String result = refresher.refreshOidcToken("OIDC");

        assertThat(result, is("token"));
    }

    @Test
    public void throwsExceptionWhenHttpStatusNotOk() {
        ResponseEntity<JsonNode> response = new ResponseEntity<>(HttpStatus.FOUND);
        when(restTemplateMock.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(JsonNode.class))).thenReturn(response);

        expectedException.expect(OidcRefreshException.class);
        expectedException.expectMessage("Unexpected Http code when refreshing. Expected 200, got: 302");

        refresher.refreshOidcToken("OIDC");
    }
}
