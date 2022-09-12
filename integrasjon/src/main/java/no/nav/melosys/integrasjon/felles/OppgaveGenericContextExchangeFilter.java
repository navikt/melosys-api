package no.nav.melosys.integrasjon.felles;

import no.nav.melosys.integrasjon.reststs.RestStsClient;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.client.spring.ClientConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
public class OppgaveGenericContextExchangeFilter extends GenericContextExchangeFilter {

    static String CLIENT_NAME = "oppgave";

    public OppgaveGenericContextExchangeFilter(RestStsClient restStsClient,
                                               ClientConfigurationProperties clientConfigurationProperties,
                                               OAuth2AccessTokenService oAuth2AccessTokenService) {
        super(restStsClient, clientConfigurationProperties, oAuth2AccessTokenService, CLIENT_NAME);
    }

}
