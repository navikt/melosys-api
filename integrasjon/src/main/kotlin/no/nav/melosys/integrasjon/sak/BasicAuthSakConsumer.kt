package no.nav.melosys.integrasjon.sak

import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.felles.BasicAuthAware
import no.nav.melosys.sikkerhet.context.SubjectHandler
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

// TODO erstatt basic auth med bruk av Entra tokens
class BasicAuthSakConsumer(
    private val webClient: WebClient
) : SakConsumerInterface, BasicAuthAware {

    override fun opprettSak(sakDto: SakDto): SakDto {
        val userID = SubjectHandler.getInstance().getUserID()
        sakDto.opprettetAv = userID

        return webClient
            .post()
            .header(HttpHeaders.AUTHORIZATION, this.auth)
            .bodyValue(sakDto)
            .retrieve()
            .bodyToMono<SakDto>()
            .block() ?: throw TekniskException("Mangler respons fra Sak")
    }

    private val auth: String?
        get() {
            if (ThreadLocalAccessInfo.shouldUseOidcToken()) {
                throw TekniskException("Sak kan kun bli kalt i fra prosess")
            }
            return basicAuth()
        }
}
