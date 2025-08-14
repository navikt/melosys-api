package no.nav.melosys.integrasjon.doksys.distribuerjournalpost

import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostRequest
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostResponse
import no.nav.melosys.integrasjon.felles.RestErrorHandler
import org.slf4j.LoggerFactory
import org.springframework.retry.annotation.Retryable
import org.springframework.web.reactive.function.client.WebClient

@Retryable
open class DistribuerJournalpostConsumerWebClientImpl(
    private val webClient: WebClient
) : RestErrorHandler(), DistribuerJournalpostConsumer {

    private val log = LoggerFactory.getLogger(DistribuerJournalpostConsumerWebClientImpl::class.java)

    override fun distribuerJournalpost(request: DistribuerJournalpostRequest): DistribuerJournalpostResponse? {
        log.info("Distribuerer journalpost {}", request.journalpostId)

        return webClient.post()
            .bodyValue(request)
            .retrieve()
            .bodyToMono(DistribuerJournalpostResponse::class.java)
            .block()
    }
}
