package no.nav.melosys.integrasjon.felles;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import no.nav.melosys.exception.TekniskException;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public interface WebClientConfig {
    default ExchangeFilterFunction errorFilter(String feilmelding) {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            if (response.statusCode().isError()) {
                return response.bodyToMono(String.class)
                    .defaultIfEmpty(response.statusCode().toString())
                    .flatMap(
                        errorBody -> Mono.error(lagException(feilmelding, response.statusCode(), errorBody)));
            }
            return Mono.just(response);
        });
    }

    default Exception lagException(String feilmelding, HttpStatusCode statusCode, String errorBody) {
        return new TekniskException(feilmelding + " " + statusCode + " - " + errorBody);
    }

    /**
     * Configurerer HttpClient med timeout-innstillinger for store filer og langvarige requests.
     * Brukes når man sender/mottar store payloads som kan ta lengre tid.
     */
    default ReactorClientHttpConnector lagHttpClientConnectorMedTimeouts() {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000) // 30 sekunder for å etablere forbindelse
            .responseTimeout(Duration.ofMinutes(5)) // 5 minutter for fullstendig respons
            .doOnConnected(conn -> conn
                .addHandlerLast(new ReadTimeoutHandler(5, TimeUnit.MINUTES)) // 5 minutter read timeout
                .addHandlerLast(new WriteTimeoutHandler(5, TimeUnit.MINUTES)) // 5 minutter write timeout
            );
        return new ReactorClientHttpConnector(httpClient);
    }
}
