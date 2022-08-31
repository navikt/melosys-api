package no.nav.melosys.integrasjon.felles;

import javax.annotation.Nonnull;

import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.aad.AzureADConsumerImpl;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.apache.cxf.rs.security.jose.jwt.JwtToken;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;

@Component
public class GenericContextExchangeFilter implements ExchangeFilterFunction {
    private final RestStsClient restStsClient;

    private final AzureADConsumerImpl azureADConsumer;

    public GenericContextExchangeFilter(RestStsClient restStsClient, AzureADConsumerImpl azureADConsumer) {
        this.restStsClient = restStsClient;
        this.azureADConsumer = azureADConsumer;
    }

    @Nonnull
    @Override
    public Mono<ClientResponse> filter(@Nonnull final ClientRequest clientRequest,
                                       @Nonnull final ExchangeFunction exchangeFunction) {

        if (ThreadLocalAccessInfo.shouldUseSystemToken()) {
            ClientRequest clientRequestWithBearerAuth = ClientRequest.from(clientRequest)
                .header(HttpHeaders.AUTHORIZATION, restStsClient.bearerToken())
                .build();
            return exchangeFunction.exchange(clientRequestWithBearerAuth);
        }

        String oidcTokenString = SubjectHandler.getInstance().getOidcTokenString();
        if (oidcTokenString == null) {
            throw new TekniskException("Token mangler fra bruker! " + ThreadLocalAccessInfo.getInfo());
        }

        // Vi skal nå hente ny token fra Azure AD for å legge på den i vår nye kall
        String issuedToken = azureADConsumer.hentToken(oidcTokenString, "api://dev-fss.oppgavehandtering.oppgave-q1/.default");

        return exchangeFunction.exchange(
            ClientRequest.from(clientRequest)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + issuedToken)
                .build()
        );
    }
}
