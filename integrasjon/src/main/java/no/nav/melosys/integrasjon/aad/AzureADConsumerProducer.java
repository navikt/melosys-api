package no.nav.melosys.integrasjon.aad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Objects;

@Configuration
public class AzureADConsumerProducer {

    public AzureADConsumerProducer() {
    }

    @Bean
    @Primary
    public AzureADConsumerImpl azureADConsumer(WebClient.Builder webClientBuilder, Environment environment) {
        WebClient.Builder builder = webClientBuilder
            .baseUrl("https://login.microsoftonline.com/966ac572-f5b7-4bbe-aa88-c76419c0f851")
            .defaultHeader("Content-Type", "application/x-www-form-urlencoded");

        if (environment.getProperty("HTTP_PROXY") == null) {
            return new AzureADConsumerImpl(builder.build(), environment);
        }
        return new AzureADConsumerImpl(
            builder.clientConnector(WebClientProxyConfig.INSTANCE.clientHttpConnector(environment.getProperty("HTTP_PROXY"))).build(),
            environment
        );
    }
}
