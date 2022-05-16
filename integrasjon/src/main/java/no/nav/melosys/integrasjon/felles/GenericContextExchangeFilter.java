package no.nav.melosys.integrasjon.felles;

import javax.annotation.Nonnull;

import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.reststs.RestSts;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

@Component
public class GenericContextExchangeFilter implements ExchangeFilterFunction {
    private final RestSts restSts;

    public GenericContextExchangeFilter(RestSts restSts) {
        this.restSts = restSts;
    }

    @Nonnull
    @Override
    public Mono<ClientResponse> filter(@Nonnull final ClientRequest clientRequest,
                                       @Nonnull final ExchangeFunction exchangeFunction) {

        if (ThreadLocalAccessInfo.shouldUseSystemToken()) {
            ClientRequest clientRequestWithBearerAuth = ClientRequest.from(clientRequest)
                .header(HttpHeaders.AUTHORIZATION, restSts.bearerToken())
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
