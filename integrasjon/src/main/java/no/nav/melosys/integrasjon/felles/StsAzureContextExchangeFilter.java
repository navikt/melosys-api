package no.nav.melosys.integrasjon.felles;

import no.nav.melosys.integrasjon.reststs.RestSTSService;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.client.spring.ClientConfigurationProperties;

public class StsAzureContextExchangeFilter extends GenericContextExchangeFilter {

    private final RestSTSService restSTSService;

    public StsAzureContextExchangeFilter(RestSTSService restSTSService, ClientConfigurationProperties clientConfigurationProperties,
                                         OAuth2AccessTokenService oAuth2AccessTokenService, String clientName) {
        super(clientConfigurationProperties, oAuth2AccessTokenService, clientName);
        this.restSTSService = restSTSService;
    }

    @Override
    protected String getSystemToken() {
        return restSTSService.bearerToken();
    }
}
