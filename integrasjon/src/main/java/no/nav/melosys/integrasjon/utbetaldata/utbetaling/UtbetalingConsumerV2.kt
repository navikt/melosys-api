package no.nav.melosys.integrasjon.utbetaldata.utbetaling

import org.springframework.http.MediaType
import org.springframework.retry.annotation.Retryable
import org.springframework.web.reactive.function.client.WebClient

@Retryable
open class UtbetalingConsumerV2(private val webClient: WebClient) {

    open fun hentUtbetalingsInformasjon(fom: String, tom: String, ident: String) = webClient.get()
        .uri("/v2/hent-utbetalingsinformasjon/intern")
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(UtbetalingResponse::class.java)
        .block()!!
}
