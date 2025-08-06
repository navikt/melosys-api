package no.nav.melosys.integrasjon.sak

import jakarta.ws.rs.client.ClientBuilder
import jakarta.ws.rs.client.Entity
import jakarta.ws.rs.client.WebTarget
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import mu.KotlinLogging
import no.nav.melosys.config.MDCOperations
import no.nav.melosys.config.MDCOperations.Companion.getCorrelationId
import no.nav.melosys.exception.IntegrasjonException
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.felles.*
import no.nav.melosys.integrasjon.sak.dto.SakDto
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.retry.annotation.Retryable
import java.security.NoSuchAlgorithmException
import javax.net.ssl.SSLContext

private val log = KotlinLogging.logger { }

@Retryable
class SakConsumer internal constructor(endpointUrl: String?) : FeilHandterer, BasicAuthAware {
    private val target: WebTarget

    init {
        try {
            val sslContext = SSLContext.getDefault()
            val client = ClientBuilder.newBuilder().sslContext(sslContext).build()
            target = client.register(JacksonObjectMapperProvider::class.java).target(endpointUrl)
        } catch (e: NoSuchAlgorithmException) {
            log.error("Feilet under oppsett av integrasjon mot Sak API", e)
            throw IntegrasjonException("Feilet under oppsett av integrasjon mot Sak API")
        }
    }

    fun hentSak(id: Long): SakDto? {
        try {
            return target
                .path(id.toString())
                .request()
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
                .header(MDCOperations.Companion.X_CORRELATION_ID, getCorrelationId())
                .header(HttpHeaders.AUTHORIZATION, this.auth)
                .get(SakDto::class.java)
        } catch (e: RuntimeException) {
            ExceptionMapper.JaxGetRuntimeExTilMelosysEx(e)
            return null // Død kode
        }
    }

    fun opprettSak(sakDto: SakDto): SakDto? {
        val userID = SubjectHandler.getInstance().getUserID()
        sakDto.opprettetAv = userID
        target
            .request()
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .header(MDCOperations.Companion.X_CORRELATION_ID, getCorrelationId())
            .header(HttpHeaders.AUTHORIZATION, this.auth)
            .post(Entity.json<SakDto?>(sakDto)).use { response ->
                håndterEvFeil(response)
                return response.readEntity<SakDto?>(SakDto::class.java)
            }
    }

    override fun håndterEvFeil(response: Response) {
        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) return
        val feilResponseDto = response.readEntity<FeilResponseDto>(FeilResponseDto::class.java)
        log.error(
            "Feil oppstod. Uuid={}, Response Kode={}, Feilmelding={}",
            feilResponseDto.getUuid(),
            response.getStatus(),
            feilResponseDto.getFeilmelding()
        )
        httpStatusTilException(response.getStatus(), feilResponseDto.getFeilmelding())
    }

    private val auth: String?
        get() {
            if (ThreadLocalAccessInfo.shouldUseOidcToken()) {
                throw TekniskException("Sak kan kun bli kalt i fra prosess")
            }
            return basicAuth()
        }
}
