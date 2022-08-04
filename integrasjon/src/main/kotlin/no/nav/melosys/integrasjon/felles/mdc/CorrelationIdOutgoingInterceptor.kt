package no.nav.melosys.integrasjon.felles.mdc

import no.nav.melosys.integrasjon.felles.mdc.MDCOperations.Companion.CORRELATION_ID
import no.nav.melosys.integrasjon.felles.mdc.MDCOperations.Companion.X_CORRELATION_ID
import org.slf4j.MDC
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpRequestExecution
import org.springframework.http.client.ClientHttpRequestInterceptor
import org.springframework.http.client.ClientHttpResponse
import java.util.*

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

    private fun getCorrelationId(): String {
        val correlationId = MDC.get(CORRELATION_ID)
        return if (correlationId.isNullOrBlank()) {
            UUID.randomUUID().toString()
        } else correlationId
    }

}
