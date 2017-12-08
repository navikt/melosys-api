package jwks;

import org.jose4j.jwk.JsonWebKeySet;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.provider.token.store.jwk.JwkException;
import org.springframework.web.client.RestTemplate;

import java.util.function.Supplier;

public class JwksSupplier implements Supplier<JsonWebKeySet> {
    private final RestTemplate restTemplate;
    private final String url;

    public JwksSupplier(RestTemplate restTemplate, String url) {
        this.restTemplate = restTemplate;
        this.url = url;
    }

    @Override
    public JsonWebKeySet get() {
        try {
            ResponseEntity<String> result = restTemplate.getForEntity(url, String.class);
            return new JsonWebKeySet(result.getBody());
        } catch (Exception e) { // Might get server or client exceptions, as well as Jose4j exceptions.
            throw new JwkException("Failed to get JWKs", e);
        }
    }
}