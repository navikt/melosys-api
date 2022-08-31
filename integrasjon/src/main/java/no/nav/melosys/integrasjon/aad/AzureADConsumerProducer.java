package no.nav.melosys.integrasjon.aad;

import no.nav.melosys.integrasjon.felles.GenericContextExchangeFilter;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class AzureADConsumerProducer {

    public AzureADConsumerProducer() {
    }

    @Bean
    @Primary
    public AzureADConsumerImpl azureADConsumer(WebClient.Builder webClientBuilder, Environment environment) {
        return new AzureADConsumerImpl(
            webClientBuilder
                .baseUrl("https://login.microsoftonline.com/966ac572-f5b7-4bbe-aa88-c76419c0f851")
                .defaultHeader("Content-Type", "application/x-www-form-urlencoded")
                .build(),
            environment
        );
    }
}
