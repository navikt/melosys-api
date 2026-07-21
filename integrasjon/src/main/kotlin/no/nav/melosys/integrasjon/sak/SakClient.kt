package no.nav.melosys.integrasjon.sak

import mu.KotlinLogging
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.sikkerhet.tilgang.SubjectHandler
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

private val log = KotlinLogging.logger { }

class SakClient(
    private val webClient: WebClient
) : SakClientInterface {

    override fun opprettSak(sakDto: SakDto): SakDto {
        sakDto.opprettetAv = SubjectHandler.getInstance().getUserID()

        log.info { "Kaller Sak API for opprettelse av sak med saksnummer ${sakDto.saksnummer} og tema ${sakDto.tema}" }

        val response = webClient
            .post()
            .bodyValue(sakDto)
            .retrieve()
            .bodyToMono<SakDto>()
            .block() ?: throw TekniskException("Mangler respons fra Sak")

        log.info { "Opprettet sak i Sak API med id ${response.id} for saksnummer ${sakDto.saksnummer}" }

        return response
    }
}
