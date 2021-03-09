package no.nav.melosys.integrasjon.joark.journalfoerinngaaende;

import no.nav.melosys.integrasjon.felles.SystemContextClientRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class JournalfoerInngaaendeConsumerProducer {

    private final String url;
    private final SystemContextClientRequestInterceptor systemContextClientRequestInterceptor;

    public JournalfoerInngaaendeConsumerProducer(@Value("${JournalfoerInngaaende_v1.url}") String url, SystemContextClientRequestInterceptor systemContextClientRequestInterceptor) {
        this.url = url;
        this.systemContextClientRequestInterceptor = systemContextClientRequestInterceptor;
    }

    @Bean
    public JournalfoerInngaaendeConsumer journalfoerInngaaendeConsumer(RestTemplateBuilder restTemplateBuilder) {
        RestTemplate restTemplate = restTemplateBuilder
            .uriTemplateHandler(new DefaultUriBuilderFactory(url))
            .interceptors(systemContextClientRequestInterceptor)
            .build();

        return new JournalfoerInngaaendeConsumer(restTemplate);
    }
}
