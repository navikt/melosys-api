package no.nav.melosys.integrasjon.aareg.arbeidsforhold;


import no.nav.melosys.integrasjon.felles.WebClientConfig;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdOutgoingFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class ArbeidsforholdRestConsumerConfig implements WebClientConfig {
    private final String url;

    public ArbeidsforholdRestConsumerConfig(@Value("${arbeidsforhold.rest.url}") String url) {
        this.url = url;
    }

    @Bean
    ArbeidsforholdRestConsumer arbeidsforholdRestConsumer(WebClient.Builder webClientBuilder,
                                                          ArbeidsforholdContextExchangeFilter systemContextExchangeFilter,
                                                          CorrelationIdOutgoingFilter correlationIdOutgoingFilter) {
        return new ArbeidsforholdRestConsumer(webClientBuilder
            .baseUrl(url)
            .filter(systemContextExchangeFilter)
            .filter(correlationIdOutgoingFilter)
            .filter(errorFilter("Henting av arbeidsforhold fra Aareg feilet"))
            .build());
    }
}
