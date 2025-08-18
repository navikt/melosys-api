package no.nav.melosys.integrasjon.soknadmottak;

import java.util.Arrays;

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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.xml.Jaxb2RootElementHttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import static no.nav.melosys.featuretoggle.ToggleName.MELOSYS_SOKNAD_MOTTAK_CONSUMER_BRUK_WEBCLIENT_MED_AD_TOKEN;

@Configuration
public class SoknadMottakConsumerProducer implements WebClientConfig {

    @Bean
    public SoknadMottakConsumer soknadMottakConsumer(
        Unleash unleash,
        WebClient.Builder webclientBuilder,
        @Value("${MelosysSoknadMottak.url}") String url,
        CorrelationIdOutgoingFilter correlationIdOutgoingFilter,
        GenericAuthFilterFactory genericAuthFilterFactory,
        SystemContextClientRequestInterceptor interceptor,
        CorrelationIdOutgoingInterceptor correlationIdOutgoingInterceptor
    ) {

        if (unleash.isEnabled(MELOSYS_SOKNAD_MOTTAK_CONSUMER_BRUK_WEBCLIENT_MED_AD_TOKEN)) {
            return new SoknadMottakConsumerImpl(
                webclientBuilder
                    .baseUrl(url)
                    .filter(genericAuthFilterFactory.getAzureFilter("melosys-soknad-mottak"))
                    .filter(correlationIdOutgoingFilter)
                    .filter(errorFilter("Kall mot søknad mottak feilet."))
                    .build()
            );
        }

        return soknadMottakConsumerOld(interceptor, correlationIdOutgoingInterceptor, url);
    }

    private SoknadMottakConsumerImplOld soknadMottakConsumerOld(
        SystemContextClientRequestInterceptor interceptor,
        CorrelationIdOutgoingInterceptor correlationIdOutgoingInterceptor,
        String url
    ) {
        RestTemplate restTemplate = new RestTemplateBuilder().rootUri(url)
            .additionalMessageConverters(new Jaxb2RootElementHttpMessageConverter(), new MappingJackson2HttpMessageConverter())
            .build();
        restTemplate.setInterceptors(Arrays.asList(interceptor, correlationIdOutgoingInterceptor));
        return new SoknadMottakConsumerImplOld(restTemplate);
    }


}
