package no.nav.melosys.integrasjon.felles;

import no.nav.security.token.support.client.core.ClientProperties;
import no.nav.security.token.support.client.core.OAuth2GrantType;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.client.spring.ClientConfigurationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AzureContextExchangeFilter extends GenericContextExchangeFilter {

    private static final Logger log = LoggerFactory.getLogger(AzureContextExchangeFilter.class);

    public AzureContextExchangeFilter(ClientConfigurationProperties clientConfigurationProperties,
                                      OAuth2AccessTokenService oAuth2AccessTokenService, String clientName) {
        super(clientConfigurationProperties, oAuth2AccessTokenService, clientName);
    }

    @Override
    protected String getSystemToken() {
        ClientProperties clientPropertiesForSystem = ClientProperties.builder()
            .tokenEndpointUrl(clientProperties.getTokenEndpointUrl())
            .scope(clientProperties.getScope())
            .authentication(clientProperties.getAuthentication())
            .grantType(OAuth2GrantType.CLIENT_CREDENTIALS)
            .build();

        String accessToken = oAuth2AccessTokenService.getAccessToken(clientPropertiesForSystem).getAccessToken();

        log.info("clientPropertiesForSystem: {}", clientPropertiesForSystem);
        log.info("token: {}", accessToken);

        return "Bearer " + accessToken;

    }
}
