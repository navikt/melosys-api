package jwks;

import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;

import java.security.Key;

public class JwksCache {
    private final JwksSupplier supplier;
    private JsonWebKeySet keyCache;

    public JwksCache(JwksSupplier supplier) {
        this.supplier = supplier;
    }

    public Key getValidationKey(String kid, String alg) {
        Key key = getCachedKey(kid, alg);
        if (key != null) {
            return key;
        }
        synchronized (supplier) {
            if (keyCache == null) {
                keyCache = supplier.get();
            }
        }
        return getCachedKey(kid, alg);
    }

    private Key getCachedKey(String kid, String alg) {
        if (keyCache == null) {
            return null;
        }
        JsonWebKey jwk = keyCache.findJsonWebKey(kid, "RSA", "sig", alg);
        if (jwk == null) {
            return null;
        }
        return jwk.getKey();
    }
}