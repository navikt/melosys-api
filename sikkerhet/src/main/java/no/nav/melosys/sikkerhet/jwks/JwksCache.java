package no.nav.melosys.sikkerhet.jwks;

import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;

import java.security.Key;

public class JwksCache {

    private static final String KEY_TYPE = "RSA";
    private static final String KEY_USE = "sig";

    private JsonWebKeySet keyCache;

    public JwksCache(JwksSupplier supplier) {
        this.keyCache = supplier.get();
    }

    public Key getValidationKey(String kid, String alg) {
        JsonWebKey jwk = keyCache.findJsonWebKey(kid, KEY_TYPE, KEY_USE, alg);
        if (jwk == null) {
            return null;
        }
        return jwk.getKey();
    }

}