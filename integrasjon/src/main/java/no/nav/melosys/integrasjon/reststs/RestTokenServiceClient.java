package no.nav.melosys.integrasjon.reststs;

import java.util.Map;

import no.nav.melosys.integrasjon.felles.BasicAuthAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@Profile("!local-mock")
public class RestTokenServiceClient extends RestTokenServiceClientBase implements BasicAuthAware {
    private final WebClient webClient;

    public RestTokenServiceClient(WebClient webClient) {
        this.webClient = webClient;
    }

    @Override
    Map<String, Object> getResponse() {
        return webClient.get()
            .uri(createUriString())
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.AUTHORIZATION, basicAuth())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
            })
            .block();
    }

    private String createUriString() {
        return UriComponentsBuilder.fromPath("")
            .queryParam("grant_type", "client_credentials")
            .queryParam("scope", "openid").toUriString();
    }
}
