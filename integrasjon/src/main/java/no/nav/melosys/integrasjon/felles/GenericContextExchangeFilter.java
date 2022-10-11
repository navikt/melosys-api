package no.nav.melosys.integrasjon.felles;

import java.util.Optional;
import javax.annotation.Nonnull;

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

public class GenericContextExchangeFilter implements ExchangeFilterFunction {
    private final RestStsClient restStsClient;

    private final OAuth2AccessTokenService oAuth2AccessTokenService;

    private final ClientProperties clientProperties;

    public GenericContextExchangeFilter(RestStsClient restStsClient,
                                        ClientConfigurationProperties clientConfigurationProperties,
                                        OAuth2AccessTokenService oAuth2AccessTokenService,
                                        String clientName) {
        this.restStsClient = restStsClient;
        this.oAuth2AccessTokenService = oAuth2AccessTokenService;
        this.clientProperties = Optional.ofNullable(clientConfigurationProperties.getRegistration().get(clientName))
            .orElseThrow(() -> new RuntimeException("Fant ikke OAuth2-config for " + clientName));
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
        return clientRequestBuilder.header(HttpHeaders.AUTHORIZATION, getCorrectToken());
    }

    protected String getCorrectToken() {
        if (ThreadLocalAccessInfo.shouldUseSystemToken()) {
            return getSystemToken();
        }
        return "Bearer " + getUserToken();
    }

    protected String getSystemToken() {
        return restStsClient.bearerToken();
    }

    private String getUserToken() {
        OAuth2AccessTokenResponse response = oAuth2AccessTokenService.getAccessToken(clientProperties);
        return response.getAccessToken();
    }
}
