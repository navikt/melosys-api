package no.nav.melosys.integrasjon.reststs;

import java.util.Map;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.metrics.MetrikkerNavn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;

@Deprecated
@Component
public class RestSTSService {

    private static final Logger log = LoggerFactory.getLogger(RestSTSService.class);
    private static final String ACCESS_TOKEN_KEY = "access_token";

    private final SecurityTokenServiceConsumer securityTokenServiceConsumer;
    private final Counter oidcTokenRequestCounter;
    private final Counter samlTokenRequestCounter;
    private final Counter oidcTokenErrorCounter;
    private final Counter samlTokenErrorCounter;
    private final Timer oidcTokenRequestTimer;
    private final Timer samlTokenRequestTimer;

    public RestSTSService(SecurityTokenServiceConsumer securityTokenServiceConsumer, MeterRegistry meterRegistry) {
        this.securityTokenServiceConsumer = securityTokenServiceConsumer;

        this.oidcTokenRequestCounter = Counter.builder(MetrikkerNavn.STS_OIDC_REQUESTS)
            .description("Number of OIDC token requests")
            .register(meterRegistry);

        this.samlTokenRequestCounter = Counter.builder(MetrikkerNavn.STS_SAML_REQUESTS)
            .description("Number of SAML token requests")
            .register(meterRegistry);

        this.oidcTokenErrorCounter = Counter.builder(MetrikkerNavn.STS_OIDC_ERRORS)
            .description("Number of OIDC token request errors")
            .register(meterRegistry);

        this.samlTokenErrorCounter = Counter.builder(MetrikkerNavn.STS_SAML_ERRORS)
            .description("Number of SAML token request errors")
            .register(meterRegistry);

        this.oidcTokenRequestTimer = Timer.builder(MetrikkerNavn.STS_OIDC_DURATION)
            .description("OIDC token request duration")
            .register(meterRegistry);

        this.samlTokenRequestTimer = Timer.builder(MetrikkerNavn.STS_SAML_DURATION)
            .description("SAML token request duration")
            .register(meterRegistry);
    }

    public String samlToken() {
        return collectSamlToken();
    }

    public String bearerToken() {
        return "Bearer " + collectBearerToken();
    }

    private synchronized String collectBearerToken() {
        try {
            return generateToken();
        } catch (Exception e) {
            throw new TekniskException("generateToken feilet", e);
        }
    }

    private synchronized String collectSamlToken() {
        try {
            return generateSamlToken();
        } catch (Exception e) {
            throw new TekniskException("generateSamlToken feilet", e);
        }
    }

    private String generateToken() throws Exception {
        log.info("Henter oidc-token fra security-token-service");
        oidcTokenRequestCounter.increment();

        return oidcTokenRequestTimer.recordCallable(() -> {
            try {
                Map<String, Object> responseBody = securityTokenServiceConsumer.getResponseForOidcToken();
                return (String) responseBody.get(ACCESS_TOKEN_KEY);

            } catch (HttpStatusCodeException e) {
                oidcTokenErrorCounter.increment();
                throw new IllegalStateException(mapException(e));
            } catch (Exception ex) {
                oidcTokenErrorCounter.increment();
                throw new IllegalStateException("Ukjent feil ved henting av OIDC-token fra STS", ex);
            }
        });
    }

    private String generateSamlToken() throws Exception {
        log.info("Henter saml-token fra security-token-service");
        samlTokenRequestCounter.increment();

        return samlTokenRequestTimer.recordCallable(() -> {
            try {
                Map<String, Object> responseBody = securityTokenServiceConsumer.getResponseForSamlToken();
                return (String) responseBody.get(ACCESS_TOKEN_KEY);

            } catch (HttpStatusCodeException e) {
                samlTokenErrorCounter.increment();
                throw new IllegalStateException(mapException(e));
            } catch (Exception ex) {
                samlTokenErrorCounter.increment();
                throw new IllegalStateException("Ukjent feil ved henting av OIDC-token fra STS", ex);
            }
        });
    }

    private static RuntimeException mapException(RestClientException ex) {
        return mapException(ex, ex.getMessage());
    }

    private static RuntimeException mapException(RestClientException ex, String feilmelding) {
        if (ex instanceof HttpStatusCodeException httpStatusCodeException) {
            return switch (HttpStatus.valueOf(httpStatusCodeException.getStatusCode().value())) {
                case FORBIDDEN, UNAUTHORIZED -> new SikkerhetsbegrensningException(feilmelding, ex);
                case NOT_FOUND -> new IkkeFunnetException(feilmelding, ex);
                case BAD_REQUEST, INTERNAL_SERVER_ERROR, METHOD_NOT_ALLOWED, SERVICE_UNAVAILABLE ->
                    throw new IntegrasjonException(feilmelding, ex);
                default -> throw new TekniskException(feilmelding, ex);
            };
        }

        throw new TekniskException(feilmelding, ex);
    }
}
