package no.nav.melosys.integrasjon.sak;

import java.util.Optional;

import no.nav.security.token.support.client.core.ClientProperties;
import no.nav.security.token.support.client.core.oauth2.OAuth2AccessTokenService;
import no.nav.security.token.support.client.spring.ClientConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SakConsumerProducer {
    private static final String CLIENT_NAME = "sak";

    private final String endpointUrl;
    private final ClientProperties clientProperties;
    private final OAuth2AccessTokenService oAuth2AccessTokenService;

    public SakConsumerProducer(
        @Value("${SakAPI_v1.url}") String endpointUrl,
        ClientConfigurationProperties clientConfigurationProperties,
        OAuth2AccessTokenService oAuth2AccessTokenService

    ) {
        this.endpointUrl = endpointUrl;
        this.clientProperties = Optional.ofNullable(clientConfigurationProperties.getRegistration().get(CLIENT_NAME))
            .orElseThrow(() -> new RuntimeException("Fant ikke OAuth2-config for " + CLIENT_NAME));
        this.oAuth2AccessTokenService = oAuth2AccessTokenService;
    }

    @Bean
    public SakConsumer sakConsumer() {
        return new SakConsumerImpl(endpointUrl, clientProperties, oAuth2AccessTokenService);
    }
}
