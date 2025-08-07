package no.nav.melosys.integrasjon.sak

import jakarta.ws.rs.client.Entity
import jakarta.ws.rs.client.WebTarget
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import mu.KotlinLogging
import no.nav.melosys.config.MDCOperations
import no.nav.melosys.config.MDCOperations.Companion.getCorrelationId
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.felles.BasicAuthAware
import no.nav.melosys.integrasjon.felles.FeilHandterer
import no.nav.melosys.integrasjon.felles.FeilResponseDto
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo

private val log = KotlinLogging.logger { }

class SakConsumer internal constructor(private val target: WebTarget) : FeilHandterer, BasicAuthAware {

    fun opprettSak(sakDto: SakDto): SakDto {
        val userID = SubjectHandler.getInstance().getUserID()
        sakDto.opprettetAv = userID
        target
            .request()
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON)
            .header(MDCOperations.X_CORRELATION_ID, getCorrelationId())
            .header(HttpHeaders.AUTHORIZATION, this.auth)
            .post(Entity.json(sakDto)).use { response ->
                håndterEvFeil(response)
                return response.readEntity(SakDto::class.java)
            }
    }

    override fun håndterEvFeil(response: Response) {
        if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) return
        val feilResponseDto = response.readEntity(FeilResponseDto::class.java)
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
