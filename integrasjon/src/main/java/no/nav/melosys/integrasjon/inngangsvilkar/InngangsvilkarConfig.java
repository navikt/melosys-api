package no.nav.melosys.integrasjon.inngangsvilkar;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

@Configuration
public class InngangsvilkarConfig {

    @Bean
    public RestTemplate inngangsVilkaarRestTemplate(@Value("${Inngangsvilkaar.url}") String url,
                                                    RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
            .uriTemplateHandler(new DefaultUriBuilderFactory(url))
            .build();
    }
}
