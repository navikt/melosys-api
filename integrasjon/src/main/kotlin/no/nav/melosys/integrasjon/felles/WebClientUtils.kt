package no.nav.melosys.integrasjon.felles

import no.nav.melosys.exception.TekniskException
import org.springframework.http.HttpStatusCode
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import reactor.core.publisher.Mono

fun errorFilter(feilmelding: String): ExchangeFilterFunction = errorFilter(feilmelding, ::lagException)

fun errorFilter(
    feilmelding: String,
    lagException: (String, HttpStatusCode, String) -> Exception
): ExchangeFilterFunction =
    ExchangeFilterFunction.ofResponseProcessor { response ->
        if (response.statusCode().isError) {
            response.bodyToMono(String::class.java)
                .defaultIfEmpty(response.statusCode().toString())
                .flatMap { errorBody ->
                    Mono.error(lagException(feilmelding, response.statusCode(), errorBody))
                }
        } else {
            Mono.just(response)
        }
    }

fun lagException(feilmelding: String, statusCode: HttpStatusCode, errorBody: String): Exception =
    TekniskException("$feilmelding $statusCode - $errorBody")
