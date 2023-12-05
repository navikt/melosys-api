package no.nav.melosys.integrasjon.reststs;

import java.util.Map;

import no.nav.melosys.integrasjon.felles.BasicAuthAware;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class SecurityTokenServiceConsumer implements BasicAuthAware {
    private final WebClient webClient;

    public SecurityTokenServiceConsumer(WebClient webClient) {
        this.webClient = webClient;
    }

    public Map<String, Object> getResponseForOidcToken() {
        return webClient.get()
            .uri(createUriStringForOidcToken())
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.AUTHORIZATION, basicAuth())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
            })
            .block();
    }

    public Map<String, Object> getResponseForSamlToken() {
        if (ThreadLocalAccessInfo.shouldUseSystemToken()) {
            return getResponseForSamlSystemToken();
        }

        return getResponseForSamlOnBehalfOfToken();
    }

    private Map<String, Object> getResponseForSamlSystemToken() {
        return webClient.get()
            .uri(UriComponentsBuilder.fromPath("/samltoken").toUriString())
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.AUTHORIZATION, basicAuth())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
            })
            .block();
    }

    private Map<String, Object> getResponseForSamlOnBehalfOfToken() {
        return webClient.post()
            .uri(createUriStringForOnBehalfOfTokenSaml())
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
            .header(HttpHeaders.AUTHORIZATION, basicAuth())
            .retrieve()
            .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
            })
            .block();
    }

    private String createUriStringForOidcToken() {
        return UriComponentsBuilder.fromPath("/token")
            .queryParam("grant_type", "client_credentials")
            .queryParam("scope", "openid").toUriString();
    }

    private String createUriStringForOnBehalfOfTokenSaml() {
        String userToken = SubjectHandler.getInstance().getOidcTokenString();

        return UriComponentsBuilder.fromPath("/token/exchange")
            .queryParam("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
            .queryParam("requested_token_type", "urn:ietf:params:oauth:token-type:saml2")
            .queryParam("subject_token_type", "urn:ietf:params:oauth:token-type:access_token")
            .queryParam("subject_token", userToken)
            .toUriString();
    }
}
