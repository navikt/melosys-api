package no.nav.melosys.integrasjon.felles;

import javax.annotation.Nonnull;

import no.finn.unleash.Unleash;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

@Component
public class SystemContextExchangeFilter implements ExchangeFilterFunction {
    private final RestStsClient restStsClient;
    private final Unleash unleash;
    private final GenericContextExchangeFilter genericContextExchangeFilter;

    public SystemContextExchangeFilter(Unleash unleash, RestStsClient restStsClient, GenericContextExchangeFilter genericContextExchangeFilter) {
        this.restStsClient = restStsClient;
        this.genericContextExchangeFilter = genericContextExchangeFilter;
        this.unleash = unleash;
    }

    @Override
    @Nonnull
    public Mono<ClientResponse> filter(@Nonnull final ClientRequest clientRequest,
                                       @Nonnull final ExchangeFunction exchangeFunction) {
        if (unleash.isEnabled("melosys.auto.token")) {
            // Vi sletter SystemContextExchangeFilter og bytter ut med AutoContextExchangeFilter når vi vet at dette funker
            return genericContextExchangeFilter.filter(clientRequest, exchangeFunction);
        }

        ClientRequest clientRequestWithBearerAuth = ClientRequest.from(clientRequest)
            .header(HttpHeaders.AUTHORIZATION, restStsClient.bearerToken())
            .build();
        return exchangeFunction.exchange(clientRequestWithBearerAuth);
    }
}
