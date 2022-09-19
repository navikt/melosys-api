package no.nav.melosys.integrasjon.felles;

import java.util.Optional;
import javax.annotation.Nonnull;

import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
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

        if (ThreadLocalAccessInfo.shouldUseSystemToken()) {
            ClientRequest clientRequestWithBearerAuth = ClientRequest.from(clientRequest)
                .header(HttpHeaders.AUTHORIZATION, restStsClient.bearerToken())
                .build();
            return exchangeFunction.exchange(clientRequestWithBearerAuth);
        }

        OAuth2AccessTokenResponse response = oAuth2AccessTokenService.getAccessToken(clientProperties);
        String issuedToken = response.getAccessToken();

        return exchangeFunction.exchange(
            ClientRequest.from(clientRequest)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + issuedToken)
                .build()
        );
    }
}
