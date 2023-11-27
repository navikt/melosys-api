package no.nav.melosys.integrasjon.reststs;

import java.util.Map;

import io.getunleash.Unleash;
import no.nav.melosys.featuretoggle.ToggleName;
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
    private final Unleash unleash;

    public RestTokenServiceClient(WebClient webClient, Unleash unleash) {
        this.webClient = webClient;
        this.unleash = unleash;
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
        var path = unleash.isEnabled(ToggleName.MELOSYS_STS_NY_PATH) ? "" : "/";
        return UriComponentsBuilder.fromPath(path)
            .queryParam("grant_type", "client_credentials")
            .queryParam("scope", "openid").toUriString();
    }
}
