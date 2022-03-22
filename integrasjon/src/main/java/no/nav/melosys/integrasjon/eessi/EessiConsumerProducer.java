package no.nav.melosys.integrasjon.eessi;

import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.integrasjon.felles.SystemContextClientRequestInterceptor;
import no.nav.melosys.integrasjon.felles.UserContextClientRequestInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class EessiConsumerProducer {

    private final String url;

    public EessiConsumerProducer(@Value("${MelosysEessi.url}") String url) {
        this.url = url;
    }

    @Bean
    @Primary
    public EessiConsumer melosysEessiConsumer(ObjectMapper objectMapper,
                                              UserContextClientRequestInterceptor interceptor,
                                              RestTemplateBuilder restTemplateBuilder) {
        RestTemplate restTemplate = restTemplateBuilder.rootUri(url).build();
        restTemplate.setInterceptors(Collections.singletonList(interceptor));
        return new EessiConsumerImpl(restTemplate, objectMapper);
    }

    @Bean
    @Qualifier("system")
    public EessiConsumer melosysEessiSystemConsumer(ObjectMapper objectMapper,
                                                    SystemContextClientRequestInterceptor interceptor,
                                                    RestTemplateBuilder restTemplateBuilder) {
        RestTemplate restTemplate = restTemplateBuilder.rootUri(url).build();
        restTemplate.setInterceptors(Collections.singletonList(interceptor));
        return new EessiConsumerImpl(restTemplate, objectMapper);
    }
}
