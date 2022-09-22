package no.nav.melosys.integrasjon.felles;

import javax.annotation.Nonnull;

import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
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
    private final RestStsClient restStsClient;

    public GenericContextExchangeFilter(RestStsClient restStsClient) {
        this.restStsClient = restStsClient;
    }

    @Nonnull
    @Override
    public Mono<ClientResponse> filter(@Nonnull final ClientRequest clientRequest,
                                       @Nonnull final ExchangeFunction exchangeFunction) {
        return exchangeFunction.exchange(
            withClientRequestBuilder(ClientRequest.from(clientRequest)).build()
        );
    }

    protected ClientRequest.Builder withClientRequestBuilder(ClientRequest.Builder clientRequestBuilder) {
        return clientRequestBuilder.header(HttpHeaders.AUTHORIZATION, getAutoToken());
    }

    protected String getAutoToken() {
        if (ThreadLocalAccessInfo.shouldUseSystemToken()) {
            return getSystemToken();
        }
        return "Bearer " + getUserToken();
    }

    protected String getSystemToken() {
        return restStsClient.bearerToken();
    }

    private String getUserToken() {
        String oidcTokenString = SubjectHandler.getInstance().getOidcTokenString();
        if (oidcTokenString == null) {
            throw new TekniskException("Token mangler fra bruker! " + ThreadLocalAccessInfo.getInfo());
        }
        return oidcTokenString;
    }
}
