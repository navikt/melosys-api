package no.nav.melosys.integrasjon.sak

import no.nav.melosys.exception.TekniskException
import no.nav.melosys.sikkerhet.context.SubjectHandler
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

class SakClient(
    private val webClient: WebClient
) : SakClientInterface {

    override fun opprettSak(sakDto: SakDto): SakDto {
        sakDto.opprettetAv = SubjectHandler.getInstance().getUserID()

        return webClient
            .post()
            .bodyValue(sakDto)
            .retrieve()
            .bodyToMono<SakDto>()
            .block() ?: throw TekniskException("Mangler respons fra Sak")
    }
}
