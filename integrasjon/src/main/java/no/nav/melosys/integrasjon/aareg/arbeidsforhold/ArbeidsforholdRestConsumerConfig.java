package no.nav.melosys.integrasjon.aareg.arbeidsforhold;


import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.felles.RestConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class ArbeidsforholdRestConsumerConfig implements RestConsumer {
    private static final Logger log = LoggerFactory.getLogger(ArbeidsforholdRestConsumerConfig.class);

    private final String url;

    @Autowired
    public ArbeidsforholdRestConsumerConfig(@Value("${arbeidsforhold.rest.url}") String url) {
        this.url = url;
    }

    @Bean
    ArbeidsforholdRestConsumer arbeidsforholdRestConsumer(WebClient.Builder webClientBuilder, ArbeidsforholdContextExchangeFilter systemContextExchangeFilter) {
        return new ArbeidsforholdRestConsumer(webClientBuilder
            .baseUrl(url)
            .filter(systemContextExchangeFilter)
            .filter(errorFilter())
            .build());
    }

    public static ExchangeFilterFunction errorFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (response.statusCode().isError()) {
                return response.createException()
                    .flatMap(errorBody -> {
                        log.error("Kall mot arreg-rest feilet. {} - {}", response.statusCode(), errorBody);
                        return Mono.error(new TekniskException("Henting av arbeidsforhold fra Aareg feilet."));
                    });
            }
            return Mono.just(response);
        });
    }
}
