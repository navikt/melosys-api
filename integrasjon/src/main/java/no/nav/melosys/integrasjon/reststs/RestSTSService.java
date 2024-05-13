package no.nav.melosys.integrasjon.reststs;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;

import no.nav.melosys.integrasjon.felles.ExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;

@Deprecated
@Component
public class RestSTSService {

    private static final Logger log = LoggerFactory.getLogger(RestSTSService.class);
    private static final Long EXPIRE_TIME_TO_REFRESH = 60L;
    private static final String ACCESS_TOKEN_KEY = "access_token";
    private static final String EXPIRES_IN_KEY = "expires_in";
    private volatile LocalDateTime expiryTimeForOidcToken = LocalDateTime.now();
    private volatile LocalDateTime expiryTimeForSamlToken = LocalDateTime.now();
    private String oidcToken;
    private String samlToken;

    private final SecurityTokenServiceConsumer securityTokenServiceConsumer;

    public RestSTSService(SecurityTokenServiceConsumer securityTokenServiceConsumer) {
        this.securityTokenServiceConsumer = securityTokenServiceConsumer;
    }

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


    private String generateToken() {
        log.info("Henter oidc-token fra security-token-service");
        try {
            Map<String, Object> responseBody = securityTokenServiceConsumer.getResponseForOidcToken();

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
            Map<String, Object> responseBody = securityTokenServiceConsumer.getResponseForSamlToken();

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
