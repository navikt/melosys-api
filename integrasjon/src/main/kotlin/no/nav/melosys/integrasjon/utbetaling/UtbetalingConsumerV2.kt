package no.nav.melosys.integrasjon.utbetaling

import no.nav.melosys.integrasjon.utbetaldata.utbetaling.UtbetalingRequest
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Retryable
import org.springframework.web.reactive.function.client.WebClient

@Retryable
open class UtbetalingConsumerV2(private val webClient: WebClient) {

    open fun hentUtbetalingsInformasjon(request: UtbetalingRequest) = webClient.post()
        //.uri("/v2/hent-utbetalingsinformasjon/intern")
        .accept(MediaType.APPLICATION_JSON)
        .bodyValue(request)
        .retrieve()
        .bodyToMono(Array<Utbetaling>::class.java)
        .block()!!.toList()
}
