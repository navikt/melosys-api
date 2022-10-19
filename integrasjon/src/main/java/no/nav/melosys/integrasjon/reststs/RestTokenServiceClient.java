package no.nav.melosys.integrasjon.reststs;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;

import no.nav.melosys.integrasjon.felles.ExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Profile("!local-mock")
public class RestTokenServiceClient implements RestStsClient {
    private static final Logger log = LoggerFactory.getLogger(RestTokenServiceClient.class);

    private static final Long EXPIRE_TIME_TO_REFRESH = 60L;
    private static final String ACCESS_TOKEN_KEY = "access_token";
    private static final String EXPIRES_IN_KEY = "expires_in";
    private volatile LocalDateTime expiryTime = LocalDateTime.now();
    private String token;
    private final WebClient webClient;

    public RestTokenServiceClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public String bearerToken() {
        return "Bearer " + collectToken();
    }

    public synchronized String collectToken() {
        if (shouldCollectNewToken()) {
            token = generateToken();
        }

        return token;
    }

    private String generateToken() {
        log.info("Henter oidc-token fra security-token-service");
        try {
            Map<String, Object> response = webClient.get()
                .uri(createUriString())
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, basicAuth())
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .block();
            Map<String, Object> responseBody = Objects.requireNonNull(response);

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

    private String createUriString() {
        return UriComponentsBuilder.fromPath("/")
            .queryParam("grant_type", "client_credentials")
            .queryParam("scope", "openid").toUriString();
    }

}
