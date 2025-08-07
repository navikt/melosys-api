package no.nav.melosys.integrasjon.altinn;

import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class SoknadMottakConsumerProducer {

    @Bean
    public SoknadMottakConsumer soknadMottakConsumer(
        WebClient.Builder webclientBuilder,
        @Value("${MelosysSoknadMottak.url}") String url,
        CorrelationIdOutgoingFilter correlationIdOutgoingFilter,
        GenericAuthFilterFactory genericAuthFilterFactory
    ) {

        return new SoknadMottakConsumerImpl(
            webclientBuilder
                .baseUrl(url)
                .filter(genericAuthFilterFactory.getAzureFilter("melosys-soknad-mottak"))
                .filter(correlationIdOutgoingFilter)
                .build()
        );
    }
}
