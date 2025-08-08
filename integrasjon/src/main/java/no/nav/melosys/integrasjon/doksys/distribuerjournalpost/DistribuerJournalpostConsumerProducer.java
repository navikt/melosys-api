package no.nav.melosys.integrasjon.doksys.distribuerjournalpost;

import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class DistribuerJournalpostConsumerProducer {

    @Bean
    public DistribuerJournalpostConsumer distribuerJournalpostConsumer(
        @Value("${DistribuerJournalpost_v1.url}") String url,
        WebClient.Builder webclientBuilder,
        CorrelationIdOutgoingFilter correlationIdOutgoingFilter,
        GenericAuthFilterFactory genericAuthFilterFactory
    ) {

        return new DistribuerJournalpostConsumer(
            webclientBuilder
                .baseUrl(url)
                .filter(genericAuthFilterFactory.getAzureFilter("dokdistfordeling"))
                .filter(correlationIdOutgoingFilter)
                .build()
        );
    }
}
