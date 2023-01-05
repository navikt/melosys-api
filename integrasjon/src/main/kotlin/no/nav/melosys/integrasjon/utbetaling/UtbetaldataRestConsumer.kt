package no.nav.melosys.integrasjon.utbetaling

import org.springframework.http.MediaType
import org.springframework.retry.annotation.Retryable
import org.springframework.web.reactive.function.client.WebClient

@Retryable
open class UtbetaldataRestConsumer(private val webClient: WebClient) {

    open fun hentUtbetalingsInformasjon(request: UtbetalingRequest) = webClient.post().uri("")
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(Array<Utbetaling>::class.java)
        .block()!!.toList()
}
