package no.nav.melosys.integrasjon.aareg.arbeidsforhold

import no.nav.melosys.integrasjon.felles.GenericContextExchangeFilter
import no.nav.melosys.integrasjon.reststs.RestStsClient
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest

@Component
class ArbeidsforholdContextExchangeFilter(restStsClient: RestStsClient) :
    GenericContextExchangeFilter(restStsClient) {

    override fun withClientRequestBuilder(clientRequestBuilder: ClientRequest.Builder): ClientRequest.Builder =
        clientRequestBuilder
            // Om vi får lagt inn "0000-ga-aa-register-konsument" i sakbehandler token kan vi benytte dette når tilgjengelig
            // https://nav-it.slack.com/archives/C01BSCJM127/p1649411252534409
            .header(HttpHeaders.AUTHORIZATION, systemToken)
            .header(NAV_CONSUMER_TOKEN, systemToken)

    companion object {
        private const val NAV_CONSUMER_TOKEN = "Nav-Consumer-Token"
    }
}
