package no.nav.melosys.integrasjon.pdl;

import java.util.function.Supplier;
import javax.annotation.Nonnull;

import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.aad.AzureADConsumerImpl;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

public class PDLAuthFilter implements ExchangeFilterFunction {
    private static final String NAV_CONSUMER_TOKEN = "Nav-Consumer-Token";

    private final RestStsClient restStsClient;

    private AzureADConsumerImpl azureADConsumer;

    public PDLAuthFilter(RestStsClient restStsClient, AzureADConsumerImpl azureADConsumer) {
        this.restStsClient = restStsClient;
        this.azureADConsumer = azureADConsumer;
    }

    @Nonnull
    @Override
    public Mono<ClientResponse> filter(@Nonnull ClientRequest clientRequest,
                                       @Nonnull ExchangeFunction exchangeFunction) {
        return exchangeFunction.exchange(
            ClientRequest.from(clientRequest)
                .header(HttpHeaders.AUTHORIZATION, getTokenSupplier(restStsClient).get())
                .header(NAV_CONSUMER_TOKEN, restStsClient.bearerToken())
                .build()
        );
    }

    private Supplier<String> getTokenSupplier(RestStsClient restStsClient) {
        if (ThreadLocalAccessInfo.shouldUseSystemToken()) {
            return restStsClient::bearerToken;
        }
        if (ThreadLocalAccessInfo.shouldUseOidcToken()) {
            return () -> "Bearer " + azureADConsumer.hentToken(SubjectHandler.getInstance().getOidcTokenString(), "api://dev-fss.pdl.pdl-api/.default");
        }
        throw new TekniskException("Uregistert kall prøver å registrere token provider");
    }
}
