package no.nav.melosys.integrasjon.aareg.arbeidsforhold;

import no.finn.unleash.Unleash;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.util.function.Supplier;

@Component
public class ArbeidsforholdContextExchangeFilter implements ExchangeFilterFunction {
    private static final String NAV_CONSUMER_TOKEN = "Nav-Consumer-Token";
    private final RestStsClient restStsClient;
    private final Unleash unleash;

    public ArbeidsforholdContextExchangeFilter(RestStsClient restStsClient, Unleash unleash) {
        this.restStsClient = restStsClient;
        this.unleash = unleash;
    }

    @Override
    @Nonnull
    public Mono<ClientResponse> filter(@Nonnull final ClientRequest clientRequest,
                                       @Nonnull final ExchangeFunction exchangeFunction) {
        return exchangeFunction.exchange(
            ClientRequest.from(clientRequest)
                .header(HttpHeaders.AUTHORIZATION, getTokenSupplier(unleash, restStsClient).get())
                .header(NAV_CONSUMER_TOKEN, restStsClient.bearerToken())
                .build()
        );
    }

    private Supplier<String> getTokenSupplier(Unleash unleash, RestStsClient restStsClient) {
        if (!unleash.isEnabled("melosys.auto.token")) {
            return restStsClient::bearerToken;
        }
        if (ThreadLocalAccessInfo.isProcessCall()) {
            return restStsClient::bearerToken;
        }
        if (ThreadLocalAccessInfo.isFrontendCall()) {
            return () -> "Bearer " + SubjectHandler.getInstance().getOidcTokenString();
        }
        throw new IllegalStateException("Må bli kalt fra frontend eller prosess");
    }

}
