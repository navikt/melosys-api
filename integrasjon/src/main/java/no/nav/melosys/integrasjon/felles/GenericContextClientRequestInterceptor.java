package no.nav.melosys.integrasjon.felles;

import java.io.IOException;
import java.util.Optional;
import javax.annotation.Nonnull;

import no.nav.melosys.integrasjon.reststs.RestStsClient;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import no.nav.security.token.support.client.core.ClientProperties;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenResponse;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.client.spring.ClientConfigurationProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

//TODO: Fjern dette og erstatt med GenericContextExchangeFilter
@Component
public class GenericContextClientRequestInterceptor implements ClientHttpRequestInterceptor {
    private final RestStsClient restStsClient;
    private OAuth2AccessTokenService oAuth2AccessTokenService;
    private ClientProperties clientProperties;

    public GenericContextClientRequestInterceptor(RestStsClient restStsClient,
                                                  ClientConfigurationProperties clientConfigurationProperties,
                                                  OAuth2AccessTokenService oAuth2AccessTokenService) {
        this.clientProperties = Optional.ofNullable(clientConfigurationProperties.getRegistration().get("melosys-eessi"))
            .orElseThrow(() -> new RuntimeException("Fant ikke OAuth2-config for melosys-eessi"));

        this.restStsClient = restStsClient;
        this.oAuth2AccessTokenService = oAuth2AccessTokenService;
    }

    @Override
    @Nonnull
    public ClientHttpResponse intercept(
        @Nonnull HttpRequest request,
        @Nonnull byte[] body,
        @Nonnull ClientHttpRequestExecution execution) throws IOException {

        if (ThreadLocalAccessInfo.shouldUseSystemToken()) {
            request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + restStsClient.collectToken());
            return execution.execute(request, body);
        }

        OAuth2AccessTokenResponse response = oAuth2AccessTokenService.getAccessToken(clientProperties);
        String accessToken = response.getAccessToken();

        request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken);
        return execution.execute(request, body);
    }
}
