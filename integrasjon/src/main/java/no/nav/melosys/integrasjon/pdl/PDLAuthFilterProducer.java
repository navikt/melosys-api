package no.nav.melosys.integrasjon.pdl;

import no.nav.melosys.integrasjon.reststs.RestStsClient;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.client.spring.ClientConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PDLAuthFilterProducer {
    @Bean
    public PDLAuthFilter pdlSystemAuthFilter(RestStsClient restStsClient,
                                             ClientConfigurationProperties clientConfigurationProperties,
                                             OAuth2AccessTokenService oAuth2AccessTokenService) {
        return new PDLAuthFilter(restStsClient, clientConfigurationProperties, oAuth2AccessTokenService);
    }
}
