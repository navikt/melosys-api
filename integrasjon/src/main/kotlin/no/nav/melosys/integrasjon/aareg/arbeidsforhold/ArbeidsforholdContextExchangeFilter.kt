package no.nav.melosys.integrasjon.aareg.arbeidsforhold

import no.nav.melosys.integrasjon.reststs.RestStsClient
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono
import java.util.function.Supplier

@Component
class ArbeidsforholdContextExchangeFilter(private val restStsClient: RestStsClient) : ExchangeFilterFunction {
    override fun filter(
        clientRequest: ClientRequest,
        exchangeFunction: ExchangeFunction
    ): Mono<ClientResponse> {
        return exchangeFunction.exchange(
            ClientRequest.from(clientRequest)
                .header(HttpHeaders.AUTHORIZATION, getTokenSupplier(restStsClient).get())
                .header(NAV_CONSUMER_TOKEN, restStsClient.bearerToken())
                .build()
        )
    }

    private fun getTokenSupplier(restStsClient: RestStsClient): Supplier<String> {
        // Om vi får lagt inn "0000-ga-aa-register-konsument" i sakbehandler token kan vi benytte dette når tilgjengelig
        // https://nav-it.slack.com/archives/C01BSCJM127/p1649411252534409
        return Supplier { restStsClient.bearerToken() }
    }

    companion object {
        private const val NAV_CONSUMER_TOKEN = "Nav-Consumer-Token"
    }
}
