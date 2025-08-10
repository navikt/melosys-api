package no.nav.melosys.integrasjon.soknadmottak;

import java.util.Arrays;

import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory;
import no.nav.melosys.integrasjon.felles.SystemContextClientRequestInterceptor;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

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
        return new SoknadMottakConsumerImplOld(restTemplate);
    }

    // TODO: Feature-toggle implementasjon av SoknadMottakConsumer
    //@Bean
    public SoknadMottakConsumer soknadMottakConsumer(
        WebClient.Builder webclientBuilder,
        @Value("${MelosysSoknadMottak.url}") String url,
        CorrelationIdOutgoingFilter correlationIdOutgoingFilter,
        GenericAuthFilterFactory genericAuthFilterFactory
    ) {

        return new SoknadMottakConsumerImpl(
            webclientBuilder
                .baseUrl(url)
                .filter(genericAuthFilterFactory.getAzureFilter("melosys-soknad-mottak"))
                .filter(correlationIdOutgoingFilter)
                .build()
        );
    }
}
