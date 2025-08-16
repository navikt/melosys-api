package no.nav.melosys.integrasjon.inngangsvilkar;

import io.getunleash.Unleash;
import no.nav.melosys.integrasjon.felles.WebClientConfig;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingInterceptor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static no.nav.melosys.featuretoggle.ToggleName.MELOSYS_INNGANGSVILKAR_CONSUMER_BRUK_WEBCLIENT;

@Configuration
public class InngangsvilkarConfig implements WebClientConfig {

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

    @Bean
    public InngangsvilkaarConsumer inngangsvilkaarConsumer(
        @Qualifier("inngangsvilkår") RestTemplate restTemplate,
        @Value("${Inngangsvilkaar.url}") String url,
        WebClient.Builder webclientBuilder,
        CorrelationIdOutgoingFilter correlationIdOutgoingFilter,
        Unleash unleash
    ) {

        if (unleash.isEnabled(MELOSYS_INNGANGSVILKAR_CONSUMER_BRUK_WEBCLIENT)) {
            return new InngangsVilkarConsumerWebclientImpl(
                webclientBuilder
                    .baseUrl(url)
                    .filter(correlationIdOutgoingFilter)
                    .filter(errorFilter("Kall mot inngangsvilkår feilet."))
                    .build()
            );
        }

        return new InngangsvilkaarConsumerImpl(restTemplate);
    }
}
