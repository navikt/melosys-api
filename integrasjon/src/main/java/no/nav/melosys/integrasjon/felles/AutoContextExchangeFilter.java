package no.nav.melosys.integrasjon.felles;

import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
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

import javax.annotation.Nonnull;

@Component
public class AutoContextExchangeFilter implements ExchangeFilterFunction {
    private final RestStsClient restStsClient;

    public AutoContextExchangeFilter(RestStsClient restStsClient) {
        this.restStsClient = restStsClient;
    }

    @Nonnull
    @Override
    public Mono<ClientResponse> filter(@Nonnull final ClientRequest clientRequest,
                                       @Nonnull final ExchangeFunction exchangeFunction) {

        if (ThreadLocalAccessInfo.isProcessCall()) {
            ClientRequest clientRequestWithBearerAuth = ClientRequest.from(clientRequest)
                .header(HttpHeaders.AUTHORIZATION, restStsClient.bearerToken())
                .build();
            return exchangeFunction.exchange(clientRequestWithBearerAuth);
        }

        String oidcTokenString = SubjectHandler.getInstance().getOidcTokenString();
        if (oidcTokenString == null) {
            throw new TekniskException("Token mangler fra bruker! " + ThreadLocalAccessInfo.getInfo());
        }
        return exchangeFunction.exchange(
            ClientRequest.from(clientRequest)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + oidcTokenString)
                .build()
        );
    }
}
