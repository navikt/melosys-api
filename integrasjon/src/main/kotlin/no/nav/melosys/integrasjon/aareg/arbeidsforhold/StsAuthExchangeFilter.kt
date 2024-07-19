package no.nav.melosys.integrasjon.aareg.arbeidsforhold

import no.nav.melosys.integrasjon.reststs.RestSTSService
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono
import java.util.function.Supplier

@Deprecated("Entra (Azure AD) må brukes")
@Component
class StsAuthExchangeFilter(private val restSTSService: RestSTSService) : ExchangeFilterFunction {
    override fun filter(
        clientRequest: ClientRequest,
        exchangeFunction: ExchangeFunction,
    ): Mono<ClientResponse> {
        return exchangeFunction.exchange(
            ClientRequest.from(clientRequest)
                .header(HttpHeaders.AUTHORIZATION, getTokenSupplier(restSTSService).get())
                .header(NAV_CONSUMER_TOKEN, restSTSService.bearerToken())
                .build()
        )
    }

    private fun getTokenSupplier(restSTSService: RestSTSService): Supplier<String> {
        // Om vi får lagt inn "0000-ga-aa-register-konsument" i sakbehandler token kan vi benytte dette når tilgjengelig
        // https://nav-it.slack.com/archives/C01BSCJM127/p1649411252534409
        return Supplier { restSTSService.bearerToken() }
    }

    companion object {
        private const val NAV_CONSUMER_TOKEN = "Nav-Consumer-Token"
    }
}
