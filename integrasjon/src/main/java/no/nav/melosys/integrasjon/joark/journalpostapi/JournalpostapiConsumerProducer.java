package no.nav.melosys.integrasjon.joark.journalpostapi;

import no.nav.melosys.integrasjon.felles.SystemContextClientRequestInterceptor;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class JournalpostapiConsumerProducer {

    private final String url;

    public JournalpostapiConsumerProducer(@Value("${JournalpostApi_v1.url}") String url) {
        this.url = url;
    }

    @Bean
    public JournalpostapiConsumer journalpostapiConsumer(
        SystemContextClientRequestInterceptor systemContextClientRequestInterceptor) {
        RestTemplate restTemplate = new RestTemplateBuilder()
            .uriTemplateHandler(new DefaultUriBuilderFactory(url))
            .interceptors(systemContextClientRequestInterceptor)
            .requestFactory(() -> new HttpComponentsClientHttpRequestFactory(HttpClients.createDefault()))
            .build();

        return new JournalpostapiConsumerImpl(restTemplate);
    }
}
