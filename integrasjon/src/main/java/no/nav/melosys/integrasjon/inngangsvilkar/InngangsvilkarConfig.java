package no.nav.melosys.integrasjon.inngangsvilkar;

import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class InngangsvilkarConfig {

    @Bean
    @Qualifier("inngangsvilkår")
    public RestTemplate inngangsVilkaarRestTemplate(@Value("${Inngangsvilkaar.url}") String url,
                                                    RestTemplateBuilder restTemplateBuilder,
                                                    CorrelationIdOutgoingInterceptor correlationIdOutgoingInterceptor) {
        return restTemplateBuilder
            .uriTemplateHandler(new DefaultUriBuilderFactory(url))
            .interceptors(correlationIdOutgoingInterceptor)
            .build();
    }
}
