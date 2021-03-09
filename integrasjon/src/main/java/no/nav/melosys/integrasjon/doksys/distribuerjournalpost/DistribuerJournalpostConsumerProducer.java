package no.nav.melosys.integrasjon.doksys.distribuerjournalpost;

import no.nav.melosys.integrasjon.felles.SystemContextClientRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class DistribuerJournalpostConsumerProducer {

    private final String url;

    public DistribuerJournalpostConsumerProducer(@Value("${DistribuerJournalpost_v1.url}") String url) {
        this.url = url;
    }

    @Bean
    public DistribuerJournalpostConsumer distribuerJournalpostConsumer(
        SystemContextClientRequestInterceptor systemContextClientRequestInterceptor,
        RestTemplateBuilder restTemplateBuilder) {
        RestTemplate restTemplate = restTemplateBuilder
            .uriTemplateHandler(new DefaultUriBuilderFactory(url))
            .interceptors(systemContextClientRequestInterceptor)
            .build();

        return new DistribuerJournalpostConsumer(restTemplate);
    }
}
