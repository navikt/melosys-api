package no.nav.melosys.integrasjon.pdl;

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
public class PDLSystemAuthFilter implements ExchangeFilterFunction {
    private static final String NAV_CONSUMER_TOKEN = "Nav-Consumer-Token";

    private final RestStsClient restStsClient;

    public PDLSystemAuthFilter(RestStsClient restStsClient) {
        this.restStsClient = restStsClient;
    }

    @Nonnull
    @Override
    public Mono<ClientResponse> filter(@Nonnull ClientRequest clientRequest,
                                       @Nonnull ExchangeFunction exchangeFunction) {
        final String bearerToken = restStsClient.bearerToken();

        return exchangeFunction.exchange(
            ClientRequest.from(clientRequest)
                .header(HttpHeaders.AUTHORIZATION, bearerToken)
                .header(NAV_CONSUMER_TOKEN, bearerToken)
                .build()
        );
    }
}
