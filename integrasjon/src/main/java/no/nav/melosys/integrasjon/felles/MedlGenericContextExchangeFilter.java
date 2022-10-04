package no.nav.melosys.integrasjon.felles;

import no.nav.melosys.integrasjon.reststs.RestStsClient;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.client.spring.ClientConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
public class MedlGenericContextExchangeFilter extends GenericContextExchangeFilter {

    private static final String CLIENT_NAME = "medl";

    public MedlGenericContextExchangeFilter(RestStsClient restStsClient,
                                            ClientConfigurationProperties clientConfigurationProperties,
                                            OAuth2AccessTokenService oAuth2AccessTokenService) {
        super(restStsClient, clientConfigurationProperties, oAuth2AccessTokenService, CLIENT_NAME);
    }

}
