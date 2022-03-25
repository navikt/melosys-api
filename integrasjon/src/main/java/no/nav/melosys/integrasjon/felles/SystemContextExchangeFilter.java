package no.nav.melosys.integrasjon.felles;

import javax.annotation.Nonnull;

import no.nav.melosys.integrasjon.reststs.RestStsClient;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

@Component
public class SystemContextExchangeFilter implements ExchangeFilterFunction {
    private static final Logger log = LoggerFactory.getLogger(SystemContextExchangeFilter.class);

    private final RestStsClient restStsClient;

    public SystemContextExchangeFilter(RestStsClient restStsClient) {
        this.restStsClient = restStsClient;
    }

    @Override
    @Nonnull
    public Mono<ClientResponse> filter(@Nonnull final ClientRequest clientRequest,
                                       @Nonnull final ExchangeFunction exchangeFunction) {
        ThreadLocalAccessInfo.fromContextExchangeFilter(clientRequest.url().toString());

        if (ThreadLocalAccessInfo.isFrontendCall()) { // Debug only
            ThreadLocalAccessInfo.warnFrontendCall(this, clientRequest.url().toString());
            log.warn("Blir kalt fra forntend\n{}", ThreadLocalAccessInfo.getInfo());
        }

        ClientRequest clientRequestWithBearerAuth = ClientRequest.from(clientRequest)
            .header(HttpHeaders.AUTHORIZATION, restStsClient.bearerToken())
            .build();
        return exchangeFunction.exchange(clientRequestWithBearerAuth);
    }
}
