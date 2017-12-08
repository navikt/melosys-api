package jwks;

import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.security.Key;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JwksCacheTest {

    @Mock
    private JwksSupplier supplierMock;

    @Mock
    private JsonWebKeySet webKeySetMock;

    @Mock
    private Key keyMock;

    private JwksCache cache;

    @Before
    public void before() {
        cache = new JwksCache(supplierMock);
        when(supplierMock.get()).thenReturn(webKeySetMock);
        JsonWebKey webKey = mock(JsonWebKey.class);
        when(webKeySetMock.findJsonWebKey(anyString(), anyString(), anyString(), anyString())).thenReturn(webKey);
        when(webKey.getKey()).thenReturn(keyMock);
    }

    @Test
    public void resolvesJsonWebKeySetOnce() {
        cache.getValidationKey("kId", "RSA256");
        cache.getValidationKey("kId", "RSA256");

        verify(supplierMock).get();
    }

    @Test
    public void callsJsonWebKeySetWithCorrectValues() {
        cache.getValidationKey("kId", "RSA256");

        verify(webKeySetMock).findJsonWebKey("kId", "RSA", "sig", "RSA256");
    }

    @Test
    public void returnsKeyFromJsonWebKeyWhenPresent() {
        Key result = cache.getValidationKey("kId", "RSA256");

        assertThat(result, is(sameInstance(keyMock)));
    }

    @Test
    public void returnsNullWhenJsonWebKeyIsNull() {
        when(webKeySetMock.findJsonWebKey(anyString(), anyString(), anyString(), anyString())).thenReturn(null);

        Key result = cache.getValidationKey("kId", "RSA256");

        assertThat(result, is(nullValue()));
    }
}