package no.nav.melosys.integrasjon.kodeverk.impl

import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.WebTarget
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import no.nav.melosys.config.MDCOperations
import no.nav.melosys.config.MDCOperations.Companion.getCorrelationId
import no.nav.melosys.integrasjon.felles.CallIdAware
import no.nav.melosys.integrasjon.felles.JacksonObjectMapperProvider
import no.nav.melosys.integrasjon.kodeverk.impl.dto.FellesKodeverkDto
import java.time.LocalDate

class KodeverkConsumerImpl internal constructor(endpointUrl: String) : CallIdAware {
    private val target: WebTarget

    init {
        val client = ClientBuilder.newBuilder().build()
        target = client.register(JacksonObjectMapperProvider::class.java).target(endpointUrl)
    }

    fun hentKodeverk(navn: String): FellesKodeverkDto {
        val path = "/$VERSJON/kodeverk/$navn/koder/betydninger"
        return target
            .path(path)
            .queryParam("ekskluderUgyldige", false)
            .queryParam("oppslagsdato", LocalDate.MIN)
            .queryParam("spraak", KodeverkRegisterImpl.BOKMÅL)
            .request()
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .header("Nav-Call-Id", getCallID())
            .header("Nav-Consumer-Id", CONSUMER_ID)
            .header(MDCOperations.X_CORRELATION_ID, getCorrelationId())
            .get(FellesKodeverkDto::class.java)
    }

    companion object {
        private const val VERSJON = "v1"
        private const val CONSUMER_ID = "srvmelosys"
    }
}
