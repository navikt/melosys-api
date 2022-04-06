package no.nav.melosys.integrasjon.felles;

import javax.annotation.Nonnull;

import no.finn.unleash.Unleash;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

@Component
public class UserContextExchangeFilter implements ExchangeFilterFunction {
    private final RestStsClient restStsClient;
    private final Unleash unleash;

    public UserContextExchangeFilter(RestStsClient restStsClient, Unleash unleash) {
        this.restStsClient = restStsClient;
        this.unleash = unleash;
    }

    @Nonnull
    @Override
    public Mono<ClientResponse> filter(@Nonnull final ClientRequest clientRequest,
                                       @Nonnull final ExchangeFunction exchangeFunction) {
        if (unleash.isEnabled("melosys.auto.token")) {
            // Vi sletter UserContextExchangeFilter og bytter ut med AutoContextExchangeFilter når vi vet at dette funker
            return new GenericContextExchangeFilter(restStsClient).filter(clientRequest, exchangeFunction);
        }

        String oidcTokenString = SubjectHandler.getInstance().getOidcTokenString();
        if (oidcTokenString == null) {
            throw new TekniskException("Token mangler! Dette kommer mest sannsynlig av at en service ment for frontend kalles fra en backend-prosess");
        }
        return exchangeFunction.exchange(
            ClientRequest.from(clientRequest)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + oidcTokenString)
                .build()
        );
    }
}
