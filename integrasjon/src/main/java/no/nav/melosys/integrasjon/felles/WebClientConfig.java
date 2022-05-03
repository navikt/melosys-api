package no.nav.melosys.integrasjon.felles;

import no.nav.melosys.exception.TekniskException;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

public interface WebClientConfig {
    default ExchangeFilterFunction errorFilter(String feilmelding) {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (response.statusCode().isError()) {
                return response.bodyToMono(String.class)
                    .defaultIfEmpty(response.statusCode().getReasonPhrase())
                    .flatMap(
                        errorBody -> Mono.error(new TekniskException(feilmelding + " " + response.statusCode() + " - " + errorBody)));
            }
            return Mono.just(response);
        });
    }
}
