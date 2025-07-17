package no.nav.melosys.integrasjon.sak

import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.felles.JsonRestIntegrasjon
import no.nav.melosys.sikkerhet.context.SubjectHandler
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

open class SakConsumer(private val webClient: WebClient) : JsonRestIntegrasjon {

    fun opprettSak(sakDto: SakDto): SakDto {
        val userID = SubjectHandler.getInstance().getUserID()
        sakDto.opprettetAv = userID

        return webClient
            .post()
            .bodyValue(sakDto)
            .retrieve()
            .bodyToMono<SakDto>()
            .block() ?: throw TekniskException("Mangler respons fra Sak")
    }
}
