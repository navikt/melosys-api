package no.nav.melosys.integrasjon.doksys.distribuerjournalpost;

import io.getunleash.Unleash;
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory;
import no.nav.melosys.integrasjon.felles.SystemContextClientRequestInterceptor;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static no.nav.melosys.featuretoggle.ToggleName.MELOSYS_DISTJOURNALPOST_BRUK_WEBCLIENT_MED_AD_TOKEN;

@Configuration
public class DistribuerJournalpostConsumerProducer {

    @Bean
    public DistribuerJournalpostConsumer distribuerJournalpostConsumer(
        @Value("${DistribuerJournalpost_v1.url}") String url,
        WebClient.Builder webclientBuilder,
        CorrelationIdOutgoingFilter correlationIdOutgoingFilter,
        GenericAuthFilterFactory genericAuthFilterFactory,
        Unleash unleash,
        SystemContextClientRequestInterceptor systemContextClientRequestInterceptor,
        RestTemplateBuilder restTemplateBuilder
    ) {

        if (unleash.isEnabled(MELOSYS_DISTJOURNALPOST_BRUK_WEBCLIENT_MED_AD_TOKEN)) {
            return new DistribuerJournalpostConsumerWebClientImpl(
                webclientBuilder
                    .baseUrl(url)
                    .filter(genericAuthFilterFactory.getAzureFilter("dokdistfordeling"))
                    .filter(correlationIdOutgoingFilter)
                    .build()
            );
        }

        RestTemplate restTemplate = restTemplateBuilder
            .uriTemplateHandler(new DefaultUriBuilderFactory(url))
            .interceptors(systemContextClientRequestInterceptor)
            .build();

        return new DistribuerJournalpostConsumerRestTemplateImpl(restTemplate);
    }
}
