package no.nav.melosys.sikkerhet.oidc;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static java.util.Collections.singletonList;

class OidcRefresher {
    static final String POST_BODY = "grant_type=refresh_token&scope=openid&realm=/&refresh_token=";

    private final RestTemplate restTemplate;

    private Environment env;

    public OidcRefresher(RestTemplate restTemplate, Environment env) {
        this.restTemplate = restTemplate;
        this.env = env;
    }

    String refreshOidcToken(String refreshToken) {
        String data = POST_BODY + refreshToken;

        HttpHeaders headers = new HttpHeaders();
        headers.put("Authorization", singletonList(createAuth()));
        headers.put("Content-type", singletonList("application/x-www-form-urlencoded"));
        HttpEntity<String> request = new HttpEntity<>(data, headers);

        ResponseEntity<JsonNode> result = restTemplate.exchange(env.getRequiredProperty("OpenIdConnect.issoHost") + "/access_token", HttpMethod.POST, request, JsonNode.class);
        if (result.getStatusCode() != HttpStatus.OK) {
            throw new OidcRefreshException("Unexpected Http code when refreshing. Expected 200, got: " + result.getStatusCode());
        }
        return result.getBody().get("id_token").asText();
    }

    private String createAuth() {
        return "Basic " + Base64.getEncoder().encodeToString(
                String.format("%s:%s", env.getRequiredProperty("OpenIdConnect.username"), env.getRequiredProperty("OpenIdConnect.password"))
                        .getBytes(StandardCharsets.UTF_8));
    }
}