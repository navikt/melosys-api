package no.nav.melosys.integrasjon.reststs;

import java.util.Map;

import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@Profile("local-mock")
public class RestTokenServiceClientMockClient extends RestTokenServiceClientBase {
    private final WebClient webClient;

    public RestTokenServiceClientMockClient(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    Map<String, Object> getResponse() {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "client_credentials");
        params.add("scope", "openid");

        return webClient.post()
            .uri("token")
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.AUTHORIZATION, basicAuth())
            .bodyValue(params)
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
            })
            .block();
    }
}
