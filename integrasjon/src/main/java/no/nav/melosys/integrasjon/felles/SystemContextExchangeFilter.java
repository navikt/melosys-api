package no.nav.melosys.integrasjon.felles;

import javax.annotation.Nonnull;

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

    public SystemContextExchangeFilter(RestStsClient restStsClient) {
        this.restStsClient = restStsClient;
    }

    @Nonnull
    @Override
    public Mono<ClientResponse> filter(@Nonnull final ClientRequest clientRequest,
                                       @Nonnull final ExchangeFunction exchangeFunction) {
        ClientRequest clientRequestWithBearerAuth;
        clientRequestWithBearerAuth = ClientRequest.from(clientRequest)
            .header(HttpHeaders.AUTHORIZATION, restStsClient.basicAuth())
            .build();
        return exchangeFunction.exchange(clientRequestWithBearerAuth);
    }
}
