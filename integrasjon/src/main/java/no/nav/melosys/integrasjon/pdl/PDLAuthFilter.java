package no.nav.melosys.integrasjon.pdl;

import no.nav.melosys.integrasjon.felles.GenericContextExchangeFilter;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.client.spring.ClientConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;

@Component
public class PDLAuthFilter extends GenericContextExchangeFilter {
    private static final String NAV_CONSUMER_TOKEN = "Nav-Consumer-Token";

    private static final String CLIENT_NAME = "pdl";

    public PDLAuthFilter(RestStsClient restStsClient,
                         ClientConfigurationProperties clientConfigurationProperties,
                         OAuth2AccessTokenService oAuth2AccessTokenService) {
        super(restStsClient, clientConfigurationProperties, oAuth2AccessTokenService, CLIENT_NAME);
    }

    @Override
    protected ClientRequest.Builder withClientRequestBuilder(ClientRequest.Builder clientRequestBuilder) {
        clientRequestBuilder.header(NAV_CONSUMER_TOKEN, getSystemToken());
        return super.withClientRequestBuilder(clientRequestBuilder);
    }
}
