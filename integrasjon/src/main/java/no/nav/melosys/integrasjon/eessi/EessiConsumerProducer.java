package no.nav.melosys.integrasjon.eessi;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.melosys.integrasjon.felles.GenericContextExchangeFilter;
import no.nav.melosys.integrasjon.felles.WebClientConfig;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class EessiConsumerProducer implements WebClientConfig {

    private final String url;

    public EessiConsumerProducer(@Value("${MelosysEessi.url}") String url) {
        this.url = url;
    }

//    @Bean
//    public EessiConsumer melosysEessiConsumer(ObjectMapper objectMapper,
//                                              GenericContextClientRequestInterceptor interceptor,
//                                              CorrelationIdOutgoingInterceptor correlationIdOutgoingInterceptor,
//                                              RestTemplateBuilder restTemplateBuilder) {
//        RestTemplate restTemplate = restTemplateBuilder.rootUri(url).build();
//        restTemplate.setInterceptors(Arrays.asList(interceptor, correlationIdOutgoingInterceptor));
//        return new EessiConsumerImpl(restTemplate, objectMapper);
//    }

    @Bean
    public EessiConsumer melosysEessiConsumer(ObjectMapper objectMapper,
                                              GenericContextExchangeFilter genericContextExchangeFilter,
                                              CorrelationIdOutgoingFilter correlationIdOutgoingFilter,
                                              WebClient.Builder webClientBuilder
    ) {
        return new EessiConsumerWebClient(webClientBuilder
            .baseUrl(url)
            .filter(genericContextExchangeFilter)
            .filter(correlationIdOutgoingFilter)
            .filter(errorFilter("kall til eessi feilet"))
            .build(), objectMapper);
    }
}
