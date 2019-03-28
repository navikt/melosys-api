package no.nav.melosys.integrasjon.joark.journalfoerinngaaende;

import no.nav.melosys.integrasjon.felles.OidcTokenClientRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class JournalfoerInngaaendeConsumerProducer {

    private final String url;
    private final OidcTokenClientRequestInterceptor oidcTokenClientRequestInterceptor;

    public JournalfoerInngaaendeConsumerProducer(@Value("${JournalfoerInngaaende_v1.url}") String url, OidcTokenClientRequestInterceptor oidcTokenClientRequestInterceptor) {
        this.url = url;
        this.oidcTokenClientRequestInterceptor = oidcTokenClientRequestInterceptor;
    }

    @Bean
    public JournalfoerInngaaendeConsumer journalfoerInngaaendeConsumer() {
        RestTemplate restTemplate = new RestTemplateBuilder()
            .uriTemplateHandler(new DefaultUriBuilderFactory(url))
            .interceptors(oidcTokenClientRequestInterceptor)
            .build();

        return new JournalfoerInngaaendeConsumer(restTemplate);
    }
}
