package no.nav.melosys.integrasjon.joark.journalpostapi;

import java.util.Collections;

import io.getunleash.Unleash;
import no.nav.melosys.integrasjon.felles.GenericAuthFilterFactory;
import no.nav.melosys.integrasjon.felles.SystemContextClientRequestInterceptor;
import no.nav.melosys.integrasjon.felles.WebClientConfig;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static no.nav.melosys.featuretoggle.ToggleName.MELOSYS_DOKARKIV_BRUK_WEBCLIENT_MED_AD_TOKEN;

@Configuration
public class JournalpostapiConsumerProducer implements WebClientConfig {

    @Bean
    public JournalpostapiConsumer journalpostapiConsumer(
        WebClient.Builder webclientBuilder,
        @Value("${JournalpostApi_v1.url}") String url,
        CorrelationIdOutgoingFilter correlationIdOutgoingFilter,
        GenericAuthFilterFactory genericAuthFilterFactory,
        Unleash unleash,
        SystemContextClientRequestInterceptor systemContextClientRequestInterceptor,
        RestTemplateBuilder restTemplateBuilder
    ) {

        if (unleash.isEnabled(MELOSYS_DOKARKIV_BRUK_WEBCLIENT_MED_AD_TOKEN)) {

            return new JournalpostapiConsumerImplWebClient(
                webclientBuilder
                    .baseUrl(url)
                    .filter(genericAuthFilterFactory.getAzureFilter("dokarkiv"))
                    .filter(correlationIdOutgoingFilter)
                    .filter(errorFilter("Kall mot journalpostapi feilet."))
                    .defaultHeaders( httpHeaders -> {
                        httpHeaders.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
                        httpHeaders.setContentType(MediaType.APPLICATION_JSON);
                    })
                    .build()
            );
        }

        RestTemplate restTemplate = restTemplateBuilder
            .uriTemplateHandler(new DefaultUriBuilderFactory(url))
            .interceptors(systemContextClientRequestInterceptor, new CorrelationIdOutgoingInterceptor())
            .requestFactory(() -> new HttpComponentsClientHttpRequestFactory())
            .build();

        return new JournalpostapiConsumerImpl(restTemplate);
    }
}
