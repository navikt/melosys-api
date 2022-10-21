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
    protected volatile LocalDateTime expiryTime = LocalDateTime.now();
    private String token;

    public String bearerToken() {
        return "Bearer " + collectToken();
    }

    public synchronized String collectToken() {
        if (shouldCollectNewToken()) {
            token = generateToken();
        }

        return token;
    }

    abstract Map<String, Object> getResponse();

    private String generateToken() {
        log.info("Henter oidc-token fra security-token-service");
        try {
            Map<String, Object> responseBody = Objects.requireNonNull(getResponse());

            expiryTime = calculateExpiryTime(responseBody);

            return (String) responseBody.get(ACCESS_TOKEN_KEY);

        } catch (HttpStatusCodeException e) {
            throw new IllegalStateException(ExceptionMapper.mapException(e));
        } catch (Exception ex) {
            throw new IllegalStateException("Ukjent feil ved henting av OIDC-token fra STS", ex);
        }
    }

    private boolean shouldCollectNewToken() {
        return LocalDateTime.now().isAfter(expiryTime);
    }

    private LocalDateTime calculateExpiryTime(Map<String, Object> responseBody) {
        long expiresIn = Long.parseLong(responseBody.get(EXPIRES_IN_KEY).toString());
        return LocalDateTime.now().plus(Duration.ofSeconds(expiresIn - EXPIRE_TIME_TO_REFRESH));
    }
}
