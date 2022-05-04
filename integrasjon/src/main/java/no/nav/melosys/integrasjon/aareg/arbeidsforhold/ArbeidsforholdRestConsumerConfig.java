package no.nav.melosys.integrasjon.aareg.arbeidsforhold;


import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.felles.WebClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
public class ArbeidsforholdRestConsumerConfig implements WebClientConfig {
    private final String url;

    public ArbeidsforholdRestConsumerConfig(@Value("${arbeidsforhold.rest.url}") String url) {
        this.url = url;
    }

    @Bean
    ArbeidsforholdRestConsumer arbeidsforholdRestConsumer(WebClient.Builder webClientBuilder,
                                                          ArbeidsforholdContextExchangeFilter systemContextExchangeFilter) {
        return new ArbeidsforholdRestConsumer(webClientBuilder
            .baseUrl(url)
            .filter(systemContextExchangeFilter)
            .filter(errorFilter())
            .build());
    }

    public static ExchangeFilterFunction errorFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (response.statusCode().isError()) {
                return response.bodyToMono(String.class)
                    .defaultIfEmpty(response.statusCode().getReasonPhrase())
                    .flatMap(errorBody -> Mono.error(new TekniskException(
                        String.format("Henting av arbeidsforhold fra Aareg feilet. %s - %s", response.statusCode(), errorBody))));
            }
            return Mono.just(response);
        });
    }
}
