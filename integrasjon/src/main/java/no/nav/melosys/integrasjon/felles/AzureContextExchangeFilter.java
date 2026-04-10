package no.nav.melosys.integrasjon.felles;

import com.nimbusds.oauth2.sdk.GrantType;
import no.nav.security.token.support.client.core.ClientProperties;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.client.spring.ClientConfigurationProperties;

public class AzureContextExchangeFilter extends GenericContextExchangeFilter {

    private final ClientProperties clientPropertiesForSystem;

    public AzureContextExchangeFilter(ClientConfigurationProperties clientConfigurationProperties,
                                      OAuth2AccessTokenService oAuth2AccessTokenService, String clientName) {
        super(clientConfigurationProperties, oAuth2AccessTokenService, clientName);

        clientPropertiesForSystem = ClientProperties.builder(
                GrantType.CLIENT_CREDENTIALS,
                clientProperties.getAuthentication()
            )
            .tokenEndpointUrl(clientProperties.getTokenEndpointUrl())
            .scope(clientProperties.getScope())
            .build();
    }

    @Override
    protected String getSystemToken() {
        return "Bearer " + oAuth2AccessTokenService.getAccessToken(clientPropertiesForSystem).getAccessToken();
    }
}
