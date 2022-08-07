package no.nav.melosys.integrasjon.felles.mdc

import no.nav.melosys.integrasjon.felles.mdc.MDCOperations.Companion.X_CORRELATION_ID
import no.nav.melosys.integrasjon.felles.mdc.MDCOperations.Companion.getCorrelationId
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.ExchangeFunction
import reactor.core.publisher.Mono

@Component
class CorrelationIdOutgoingFilter : ExchangeFilterFunction {

    override fun filter(request: ClientRequest, next: ExchangeFunction): Mono<ClientResponse> {
        return next.exchange(
            ClientRequest.from(request)
                .header(X_CORRELATION_ID, getCorrelationId())
                .build()
        )
    }
}
