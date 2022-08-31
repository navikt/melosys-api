package no.nav.melosys.integrasjon.pdl;

import no.nav.melosys.integrasjon.aad.AzureADConsumerImpl;
import no.nav.melosys.integrasjon.reststs.RestStsClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PDLAuthFilterProducer {
    @Bean
    public PDLAuthFilter pdlSystemAuthFilter(RestStsClient restStsClient, AzureADConsumerImpl azureADConsumer) {
        return new PDLAuthFilter(restStsClient, azureADConsumer);
    }
}
