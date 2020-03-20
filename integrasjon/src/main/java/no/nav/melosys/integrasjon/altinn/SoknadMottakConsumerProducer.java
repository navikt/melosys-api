package no.nav.melosys.integrasjon.altinn;

import java.util.Collections;

import no.nav.melosys.integrasjon.felles.SystemContextClientRequestInterceptor;
import no.nav.melosys.integrasjon.felles.UserContextClientRequestInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

public class SoknadMottakConsumerProducer {

    private final String url;

    public SoknadMottakConsumerProducer(@Value("${MelosysSoknadMottak.url}") String url) {
        this.url = url;
    }

    @Bean
    @Primary
    public SoknadMottakConsumer soknadMottakConsumer(UserContextClientRequestInterceptor interceptor) {
        RestTemplate restTemplate = new RestTemplateBuilder().rootUri(url).build();
        restTemplate.setInterceptors(Collections.singletonList(interceptor));
        return new SoknadMottakConsumerImpl(restTemplate);
    }

    @Bean
    @Qualifier("system")
    public SoknadMottakConsumer soknadMottakSystemConsumer(SystemContextClientRequestInterceptor interceptor) {
        RestTemplate restTemplate = new RestTemplateBuilder().rootUri(url).build();
        restTemplate.setInterceptors(Collections.singletonList(interceptor));
        return new SoknadMottakConsumerImpl(restTemplate);
    }
}
