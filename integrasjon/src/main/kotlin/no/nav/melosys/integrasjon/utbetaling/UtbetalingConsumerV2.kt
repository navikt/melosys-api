package no.nav.melosys.integrasjon.utbetaling

import org.springframework.http.MediaType
import org.springframework.retry.annotation.Retryable
import org.springframework.web.reactive.function.client.WebClient

@Retryable
open class UtbetalingConsumerV2(private val webClient: WebClient) {

    open fun hentUtbetalingsInformasjon(ident: String, fom: String, tom: String) = webClient.get()
        .uri("/v2/hent-utbetalingsinformasjon/intern")
        .accept(MediaType.APPLICATION_JSON)
        .retrieve()
        .bodyToMono(Array<Utbetaling>::class.java)
        .block()!!.toList()
}
