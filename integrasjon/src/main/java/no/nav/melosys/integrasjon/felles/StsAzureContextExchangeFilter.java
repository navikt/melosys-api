package no.nav.melosys.integrasjon.felles;

import no.nav.melosys.integrasjon.reststs.RestStsClient;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.client.spring.ClientConfigurationProperties;

public class StsAzureContextExchangeFilter extends GenericContextExchangeFilter {

    private final RestStsClient restStsClient;

    public StsAzureContextExchangeFilter(RestStsClient restStsClient, ClientConfigurationProperties clientConfigurationProperties,
                                         OAuth2AccessTokenService oAuth2AccessTokenService, String clientName) {
        super(clientConfigurationProperties, oAuth2AccessTokenService, clientName);
        this.restStsClient = restStsClient;
    }

    @Override
    protected String getSystemToken() {
        return restStsClient.bearerToken();
    }
}
