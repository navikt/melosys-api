package no.nav.melosys.integrasjon.altinn;

import java.util.Arrays;

import no.nav.melosys.integrasjon.felles.SystemContextClientRequestInterceptor;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SoknadMottakConsumerProducer {

    private final String url;

    public SoknadMottakConsumerProducer(@Value("${MelosysSoknadMottak.url}") String url) {
        this.url = url;
    }

    @Bean
    public SoknadMottakConsumer soknadMottakConsumer(SystemContextClientRequestInterceptor interceptor,
                                                     CorrelationIdOutgoingInterceptor correlationIdOutgoingInterceptor) {
        RestTemplate restTemplate = new RestTemplateBuilder().rootUri(url)
            .additionalMessageConverters(new Jaxb2RootElementHttpMessageConverter(), new MappingJackson2HttpMessageConverter())
            .build();
        restTemplate.setInterceptors(Arrays.asList(interceptor, correlationIdOutgoingInterceptor));
        return new SoknadMottakConsumerImpl(restTemplate);
    }
}
