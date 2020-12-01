package no.nav.melosys.integrasjon.altinn;

import java.util.Collections;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationModule;
import no.nav.melosys.integrasjon.felles.SystemContextClientRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.xml.MappingJackson2XmlHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

@Configuration
public class SoknadMottakConsumerProducer {

    private final String url;

    public SoknadMottakConsumerProducer(@Value("${MelosysSoknadMottak.url}") String url) {
        this.url = url;
    }

    @Bean
    public SoknadMottakConsumer soknadMottakConsumer(SystemContextClientRequestInterceptor interceptor) {
        RestTemplate restTemplate = new RestTemplateBuilder().rootUri(url).build();
        restTemplate.setInterceptors(Collections.singletonList(interceptor));
        restTemplate.getMessageConverters().add(0, new MappingJackson2XmlHttpMessageConverter(
            new XmlMapper().registerModule(new JaxbAnnotationModule())
        ));
        return new SoknadMottakConsumerImpl(restTemplate);
    }
}
