package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import no.nav.melosys.integrasjon.reststs.RestStsClient;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;

@Component
public class ArbeidsforholdContextExchangeFilter implements ExchangeFilterFunction {
    private final RestStsClient restStsClient;

    public ArbeidsforholdContextExchangeFilter(RestStsClient restStsClient) {
        this.restStsClient = restStsClient;
    }

    @Override
    @Nonnull
    public Mono<ClientResponse> filter(@Nonnull final ClientRequest clientRequest,
                                       @Nonnull final ExchangeFunction exchangeFunction) {
        ClientRequest clientRequestWithBearerAuth = ClientRequest.from(clientRequest)
            .header(HttpHeaders.AUTHORIZATION, restStsClient.bearerToken())
            .header("Nav-Consumer-Token", restStsClient.bearerToken())
            .build();
        return exchangeFunction.exchange(clientRequestWithBearerAuth);
    }
}
