package no.nav.melosys.integrasjon.reststs;

import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
@Profile("local-mock")
public class RestTokenServiceClientMockClient extends RestTokenServiceClientBase {
    private final RestTemplate restTemplate;

    public RestTokenServiceClientMockClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    Map<String, Object> getResponse() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "client_credentials");
        params.add("scope", "openid");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        headers.add(HttpHeaders.AUTHORIZATION, basicAuth());

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(params, headers);

        return restTemplate.<Map<String, Object>>exchange(
            "/token", HttpMethod.POST, entity, new ParameterizedTypeReference<>() {
            }
        ).getBody();
    }
}
