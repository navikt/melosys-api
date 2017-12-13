package no.nav.melosys.sikkerhet.jwks;

import org.jose4j.jwk.JsonWebKeySet;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.oauth2.provider.token.store.jwk.JwkException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JwksSupplierTest {
    // Må være noe valid ser det ut som
    private static final String GET_RESPONSE = "{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"SH1IeRSk1OUFH3swZ+EuUq19TvQ=\",\"use\":\"sig\",\"alg\":\"RS256\",\"n\":\"AM2uHZfbHbDfkCTG8GaZO2zOBDmL4sQgNzCSFdqlQ-ikAwTV5ptyAHYC3JEy_LtMcRSv3E7r0yCW_7WtzT-CgBYQilb_lz1JmED3TgiThEolN2kaciY06UGycSj8wEYik-3PxuVeKr3uw6LVEohM3rrCjdlkQ_jctuvuUrCedbsb2hVw6Q17PQbWURq8v3gtXmGMD8KcR7e0dtf0ZoMOfZQoFJZ-a5dMFzXeP8Ffz_c0uBLSddd-\",\"e\":\"AQAB\"}]}";
    private static final String URL = "url";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Mock
    private RestTemplate restTemplateMock;

    private JwksSupplier supplier;

    @Before
    public void before() {
        supplier = new JwksSupplier(restTemplateMock, URL);
        ResponseEntity<String> response = new ResponseEntity<>(GET_RESPONSE, HttpStatus.OK);
        when(restTemplateMock.getForEntity(URL, String.class)).thenReturn(response);
    }

    @Test
    public void callsRestTemplateForUrl() {
        supplier.get();

        verify(restTemplateMock).getForEntity(URL, String.class);
    }

    @Test
    public void returnsJsonWebKeySetWithRsa() {
        JsonWebKeySet result = supplier.get();

        assertThat(result.getJsonWebKeys(), hasSize(1));
    }

    @Test
    public void throwsExceptionWhenFailToGetJwks() {
        IllegalArgumentException ex = new IllegalArgumentException();
        when(restTemplateMock.getForEntity(URL, String.class)).thenThrow(ex);

        expectedException.expect(JwkException.class);
        expectedException.expectCause(sameInstance(ex));
        expectedException.expectMessage("Failed to get JWKs");

        supplier.get();
    }
}