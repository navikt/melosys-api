package no.nav.melosys.integrasjon.aad;

import com.nimbusds.jose.shaded.json.JSONObject;
import no.nav.melosys.integrasjon.felles.FeilResponseDto;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class AzureADConsumerImpl implements AzureADConsumer {

    private final WebClient webClient;

    private final Environment environment;

    public AzureADConsumerImpl(WebClient webClient, Environment environment) {
        this.webClient = webClient;
        this.environment = environment;
    }

    @Override
    public String hentToken(String tidligereToken, String scope) {

        MultiValueMap<String, String> bodyValues = new LinkedMultiValueMap<>();
        bodyValues.add("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer");
        bodyValues.add("client_id", environment.getProperty("AZURE_APP_CLIENT_ID"));
        bodyValues.add("client_secret", environment.getProperty("AZURE_APP_CLIENT_SECRET"));
        bodyValues.add("assertion", tidligereToken);
        bodyValues.add("scope", scope);
        bodyValues.add("requested_token_use", "on_behalf_of");
        JSONObject response = webClient.post()
            .uri("/oauth2/v2.0/token")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(BodyInserters.fromFormData(bodyValues))
            .retrieve()
            .bodyToMono(JSONObject.class)
            .block();
        return response.get("access_token").toString();
    }

    private Mono<Exception> håndterFeil(ClientResponse clientResponse) {
        final HttpStatus status = clientResponse.statusCode();
        return clientResponse.bodyToMono(FeilResponseDto.class)
            .map(FeilResponseDto::getFeilmelding)
            .map(feilmelding -> tilException(feilmelding, status));
    }
}
