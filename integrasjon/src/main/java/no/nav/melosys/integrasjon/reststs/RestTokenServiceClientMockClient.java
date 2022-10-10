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
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

@Service
@Profile("local-mock")
public class RestTokenServiceClientMockClient implements RestStsClient {

    private static final Logger log = LoggerFactory.getLogger(RestTokenServiceClientMockClient.class);

    private static final Long EXPIRE_TIME_TO_REFRESH = 60L;
    private static final String ACCESS_TOKEN_KEY = "access_token";
    private static final String EXPIRES_IN_KEY = "expires_in";
    private volatile LocalDateTime expiryTime = LocalDateTime.now();
    private String token;
    private final RestTemplate restTemplate;

    public RestTokenServiceClientMockClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
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
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "client_credentials");
        params.add("scope", "openid");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.add(HttpHeaders.AUTHORIZATION, basicAuth());

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate
                .exchange("/token", HttpMethod.POST, entity, new ParameterizedTypeReference<>() {
                });

            Map<String, Object> responseBody = Objects.requireNonNull(response.getBody());
            setExpiryTime(Long.parseLong(responseBody.get(EXPIRES_IN_KEY).toString()));

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

    private void setExpiryTime(long expiryTime) {
        this.expiryTime = LocalDateTime.now().plus(Duration.ofSeconds(expiryTime - EXPIRE_TIME_TO_REFRESH));
    }
}
