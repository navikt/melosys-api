package no.nav.melosys.integrasjon.dokgen;

import no.nav.melosys.integrasjon.felles.SystemContextClientRequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;


@Configuration
public class DokgenConfig {

    @Value("${melosysdokgen.v1.url}")
    private String url;

    @Bean
    public WebClient dokgenWebClient() {
        return WebClient.create(url);
    }
}
