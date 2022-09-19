package no.nav.melosys.integrasjon.pdl;

import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nonnull;

import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import no.nav.security.token.support.client.core.ClientProperties;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.client.spring.ClientConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import reactor.core.publisher.Mono;

//TODO: Fjern PDLAuthFilter og bruk GenericContextExchangeFilter
public class PDLAuthFilter implements ExchangeFilterFunction {
    private static final String NAV_CONSUMER_TOKEN = "Nav-Consumer-Token";

    private final RestStsClient restStsClient;

    private OAuth2AccessTokenService oAuth2AccessTokenService;
    private ClientProperties clientProperties;

    public PDLAuthFilter(RestStsClient restStsClient,
                         ClientConfigurationProperties clientConfigurationProperties,
                         OAuth2AccessTokenService oAuth2AccessTokenService) {
        this.clientProperties = Optional.ofNullable(clientConfigurationProperties.getRegistration().get("pdl"))
            .orElseThrow(() -> new RuntimeException("Fant ikke OAuth2-config for pdl"));

        this.restStsClient = restStsClient;
        this.oAuth2AccessTokenService = oAuth2AccessTokenService;
    }

    @Nonnull
    @Override
    public Mono<ClientResponse> filter(@Nonnull ClientRequest clientRequest,
                                       @Nonnull ExchangeFunction exchangeFunction) {
        ClientRequest.Builder requestBuilder = ClientRequest.from(clientRequest).header(HttpHeaders.AUTHORIZATION, getTokenSupplier(restStsClient).get());

        if (ThreadLocalAccessInfo.shouldUseSystemToken()) {
            requestBuilder = requestBuilder.header(NAV_CONSUMER_TOKEN, restStsClient.bearerToken());
        }

        return exchangeFunction.exchange(requestBuilder.build());
    }

    private Supplier<String> getTokenSupplier(RestStsClient restStsClient) {
        if (ThreadLocalAccessInfo.shouldUseSystemToken()) {
            return restStsClient::bearerToken;
        }
        if (ThreadLocalAccessInfo.shouldUseOidcToken()) {
            OAuth2AccessTokenResponse response = oAuth2AccessTokenService.getAccessToken(clientProperties);
            return () -> "Bearer " + response.getAccessToken();
        }
        throw new TekniskException("Uregistert kall prøver å registrere token provider");
    }
}
