package no.nav.melosys.integrasjon.reststs;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

import no.nav.melosys.integrasjon.felles.ExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.HttpStatusCodeException;

public abstract class RestTokenServiceClientBase implements RestStsClient {
    private static final Logger log = LoggerFactory.getLogger(RestTokenServiceClientBase.class);
    protected static final Long EXPIRE_TIME_TO_REFRESH = 60L;
    protected static final String ACCESS_TOKEN_KEY = "access_token";
    protected static final String EXPIRES_IN_KEY = "expires_in";
    protected volatile LocalDateTime expiryTimeForOidcToken = LocalDateTime.now();
    protected volatile LocalDateTime expiryTimeForSamlToken = LocalDateTime.now();
    private String oidcToken;
    private String samlToken;

    public String samlToken() {
        return collectSamlToken();
    }

    public String bearerToken() {
        return "Bearer " + collectBearerToken();
    }

    private synchronized String collectBearerToken() {
        if (shouldCollectNewOidcToken()) {
            oidcToken = generateToken();
        }

        return oidcToken;
    }

    private synchronized String collectSamlToken() {
        if (shouldCollectNewSamlToken()) {
            samlToken = generateSamlToken();
        }

        return samlToken;
    }

    abstract Map<String, Object> getResponseForOidcToken();
    abstract Map<String, Object> getResponseForSamlToken();

    private String generateToken() {
        log.info("Henter oidc-token fra security-token-service");
        try {
            Map<String, Object> responseBody = Objects.requireNonNull(getResponseForOidcToken());

            expiryTimeForOidcToken = calculateExpiryTime(responseBody);

            return (String) responseBody.get(ACCESS_TOKEN_KEY);

        } catch (HttpStatusCodeException e) {
            throw new IllegalStateException(ExceptionMapper.mapException(e));
        } catch (Exception ex) {
            throw new IllegalStateException("Ukjent feil ved henting av OIDC-token fra STS", ex);
        }
    }

    private String generateSamlToken() {
        log.info("Henter saml-token fra security-token-service");
        try {
            Map<String, Object> responseBody = Objects.requireNonNull(getResponseForSamlToken());

            expiryTimeForSamlToken = calculateExpiryTime(responseBody);

            return (String) responseBody.get(ACCESS_TOKEN_KEY);

        } catch (HttpStatusCodeException e) {
            throw new IllegalStateException(ExceptionMapper.mapException(e));
        } catch (Exception ex) {
            throw new IllegalStateException("Ukjent feil ved henting av OIDC-token fra STS", ex);
        }
    }

    private boolean shouldCollectNewOidcToken() {
        return LocalDateTime.now().isAfter(expiryTimeForOidcToken);
    }

    private boolean shouldCollectNewSamlToken() {
        return LocalDateTime.now().isAfter(expiryTimeForSamlToken);
    }

    private LocalDateTime calculateExpiryTime(Map<String, Object> responseBody) {
        long expiresIn = Long.parseLong(responseBody.get(EXPIRES_IN_KEY).toString());
        return LocalDateTime.now().plus(Duration.ofSeconds(expiresIn - EXPIRE_TIME_TO_REFRESH));
    }
}
