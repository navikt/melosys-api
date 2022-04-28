package no.nav.melosys.integrasjon.pdl;

import java.util.function.Supplier;
import javax.annotation.Nonnull;

import no.finn.unleash.Unleash;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

public class PDLAuthFilter implements ExchangeFilterFunction {
    private static final String NAV_CONSUMER_TOKEN = "Nav-Consumer-Token";

    private final RestStsClient restStsClient;
    private final Supplier<String> authSupplier;
    private final Unleash unleash;

    public PDLAuthFilter(RestStsClient restStsClient, Supplier<String> authSupplier, Unleash unleash) {
        this.restStsClient = restStsClient;
        this.authSupplier = authSupplier;
        this.unleash = unleash;
    }

    @Nonnull
    @Override
    public Mono<ClientResponse> filter(@Nonnull ClientRequest clientRequest,
                                       @Nonnull ExchangeFunction exchangeFunction) {
        return exchangeFunction.exchange(
            ClientRequest.from(clientRequest)
                .header(HttpHeaders.AUTHORIZATION, getTokenSupplier(unleash, restStsClient).get())
                .header(NAV_CONSUMER_TOKEN, restStsClient.bearerToken())
                .build()
        );
    }

    private Supplier<String> getTokenSupplier(Unleash unleash, RestStsClient restStsClient) {
        if (!unleash.isEnabled("melosys.auto.token")) {
            return authSupplier;
        }
        if (ThreadLocalAccessInfo.useSystemToken()) {
            return restStsClient::bearerToken;
        }
        if (ThreadLocalAccessInfo.useOicdToken()) {
            return () -> "Bearer " + SubjectHandler.getInstance().getOidcTokenString();
        }
        throw new IllegalStateException("Må bli kalt fra frontend eller prosess");
    }
}
