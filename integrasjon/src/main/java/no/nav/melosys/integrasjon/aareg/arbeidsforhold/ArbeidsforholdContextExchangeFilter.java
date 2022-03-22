package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(ArbeidsforholdContextExchangeFilter.class);
    private final RestStsClient restStsClient;

    public ArbeidsforholdContextExchangeFilter(RestStsClient restStsClient) {
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

        String token = restStsClient.bearerToken();
        ClientRequest clientRequestWithBearerAuth = ClientRequest.from(clientRequest)
            .header(HttpHeaders.AUTHORIZATION, token)
            .header("Nav-Consumer-Token", token)
            .build();
        return exchangeFunction.exchange(clientRequestWithBearerAuth);
    }
}
