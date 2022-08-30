package no.nav.melosys.integrasjon.aad;

import no.nav.melosys.integrasjon.felles.FeilResponseDto;
import no.nav.melosys.integrasjon.oppgave.konsument.dto.OppgaveDto;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

public class AzureADConsumerImpl implements AzureADConsumer {

    private final WebClient webClient;

    public AzureADConsumerImpl(WebClient webClient) {
        this.webClient = webClient;
    }

    /*spring.security.oauth2.client.registration.azure.authorization-grant-type=authorization_code
    spring.security.oauth2.client.registration.azure.provider=azure
    spring.security.oauth2.client.provider.azure.issuer-uri=https://login.microsoftonline.com/966ac572-f5b7-4bbe-aa88-c76419c0f851/v2.0
    spring.security.oauth2.client.registration.azure.client-id=25b32992-92b5-49f0-aec8-8c201464348b
    spring.security.oauth2.client.registration.azure.client-secret=eMn8Q~0PL0wGS0gOiGhbW.rvjsoXaBskJhpNzbbF
    spring.security.oauth2.client.registration.azure.scope=api://dev-fss.oppgavehandtering.oppgave-q1/.default*/
    @Override
    public String hentToken(String tidligereToken, String scope) {
        return webClient.post()
            .uri("/token")
            .attribute("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
            .attribute("client_id", "25b32992-92b5-49f0-aec8-8c201464348b")
            .attribute("client_secret", "eMn8Q~0PL0wGS0gOiGhbW.rvjsoXaBskJhpNzbbF")
            .attribute("assertion", tidligereToken)
            .attribute("scope", scope)
            .attribute("requested_token_use", "on_behalf_of")
            .retrieve()
            .onStatus(HttpStatus::isError, this::håndterFeil)
            .bodyToMono(String.class)
            .block();
    }

    private Mono<Exception> håndterFeil(ClientResponse clientResponse) {
        final HttpStatus status = clientResponse.statusCode();
        return clientResponse.bodyToMono(FeilResponseDto.class)
            .map(FeilResponseDto::getFeilmelding)
            .map(feilmelding -> tilException(feilmelding, status));
    }
}
