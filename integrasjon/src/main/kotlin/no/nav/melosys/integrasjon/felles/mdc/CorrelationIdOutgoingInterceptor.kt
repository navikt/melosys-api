package no.nav.melosys.integrasjon.felles.mdc

import no.nav.melosys.MDCOperations.Companion.X_CORRELATION_ID
import no.nav.melosys.MDCOperations.Companion.getCorrelationId
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import org.springframework.stereotype.Component

/*
Interceptor for rest kall som bruker RestTemplate og legger på correlationId
 */
@Component
class CorrelationIdOutgoingInterceptor : ClientHttpRequestInterceptor {

    override fun intercept(
        request: HttpRequest,
        body: ByteArray,
        execution: ClientHttpRequestExecution
    ): ClientHttpResponse {
        val correlationId = getCorrelationId()
        request.headers.add(X_CORRELATION_ID, correlationId)
        return execution.execute(request, body)
    }

}
