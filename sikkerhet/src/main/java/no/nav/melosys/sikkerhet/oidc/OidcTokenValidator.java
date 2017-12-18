package no.nav.melosys.sikkerhet.oidc;

import no.nav.melosys.sikkerhet.jwks.JwksCache;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.MalformedClaimException;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtContext;
import org.mitre.openid.connect.config.ServerConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.security.Key;

/**
 * Valideringsregler er tatt fra Foreldrepenger siden prosjektet har referanseimplementasjonen i NAV:
 * http://stash.devillo.no/projects/VEDFP/repos/vl-felles/browse/sikkerhet/src/main/java/no/nav/vedtak/sikkerhet/oidc/OidcTokenValidator.java
 * <p>
 * Note: Vi skal ikke validere på Audience! Dette er noe arkitektur har satt pga. bruk av OIDC tokens på tvers.
 */
@Component
public class OidcTokenValidator {

    private JwksCache jwksCache;

    private JwtConsumerFactory jwtConsumerFactory;

    private String issuer;

    @Autowired
    public OidcTokenValidator(JwksCache jwksCache, JwtConsumerFactory jwtConsumerFactory, ServerConfiguration serverConfig) {
        this.jwksCache = jwksCache;
        this.jwtConsumerFactory = jwtConsumerFactory;
        this.issuer = serverConfig.getIssuer();
    }

    public void validate(String token) throws InvalidJwtException, MalformedClaimException {
        Assert.notNull(token, "OIDC Token cannot be null");

        JwtContext unvalidatedContext = getUnvalidatedContext(token);

        String kid = extractKid(unvalidatedContext);
        String alg = extractAlgorithm(unvalidatedContext);

        Key sigKey = jwksCache.getValidationKey(kid, alg);

        Assert.notNull(sigKey, String.format("Jwt with keyId %s and algorithm %s is not in JWKs", kid, alg));

        JwtConsumer validatingConsumer = jwtConsumerFactory.createValidatingConsumer(sigKey, issuer);

        JwtClaims claims = validatingConsumer.processToClaims(token);
        validateAzp(claims);
    }

    private JwtContext getUnvalidatedContext(String oidc) throws InvalidJwtException {
        JwtConsumer jwtConsumer = jwtConsumerFactory.createNonValidatingConsumer();
        return jwtConsumer.process(oidc);
    }

    private String extractKid(JwtContext context) {
        String kid = context.getJoseObjects().get(0).getKeyIdHeaderValue();
        return kid == null ? "" : kid;
    }

    private String extractAlgorithm(JwtContext context) {
        return context.getJoseObjects().get(0).getAlgorithmHeaderValue();
    }

    //Validates some of the rules set in OpenID Connect Core 1.0 incorporating errata set 1,
    //which is not already validated by using JwtConsumer. Ref: Foreldrepenger.
    private void validateAzp(JwtClaims claims) throws MalformedClaimException {
        String value = claims.getClaimValue("azp", String.class);
        Assert.notNull(value, "An Azp claim is required");
    }
}