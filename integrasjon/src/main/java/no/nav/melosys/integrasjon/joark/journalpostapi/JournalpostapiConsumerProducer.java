package no.nav.melosys.integrasjon.joark.journalpostapi;

import no.nav.melosys.integrasjon.felles.OidcTokenClientRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
        OidcTokenClientRequestInterceptor oidcTokenClientRequestInterceptor) {
        RestTemplate restTemplate = new RestTemplateBuilder()
            .uriTemplateHandler(new DefaultUriBuilderFactory(url))
            .interceptors(oidcTokenClientRequestInterceptor)
            .build();

        return new JournalpostapiConsumerImpl(restTemplate);
    }
}
