package no.nav.melosys.integrasjon.felles;

import no.nav.melosys.exception.TekniskException;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

public interface WebClientConfig {
    default ExchangeFilterFunction errorFilter(String feilmelding) {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (response.statusCode().isError()) {
                return response.bodyToMono(String.class)
                    .defaultIfEmpty(response.statusCode().getReasonPhrase())
                    .flatMap(
                        errorBody -> Mono.error(lagException(feilmelding, response.statusCode(), errorBody)));
            }
            return Mono.just(response);
        });
    }

    default Exception lagException(String feilmelding, HttpStatus statusCode, String errorBody) {
        return new TekniskException(feilmelding + " " + statusCode + " - " + errorBody);
    }
}
