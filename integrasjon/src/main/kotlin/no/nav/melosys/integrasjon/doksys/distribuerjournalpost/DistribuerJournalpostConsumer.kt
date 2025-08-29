package no.nav.melosys.integrasjon.doksys.distribuerjournalpost

import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostRequest
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostResponse
import org.slf4j.LoggerFactory
import org.springframework.retry.annotation.Retryable
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Retryable
@Service
class DistribuerJournalpostConsumer(
    private val distribuerJournalpostWebClient: WebClient
) {

    private val log = LoggerFactory.getLogger(DistribuerJournalpostConsumer::class.java)

    fun distribuerJournalpost(request: DistribuerJournalpostRequest): DistribuerJournalpostResponse? {
        log.info("Distribuerer journalpost {}", request.journalpostId)

        return distribuerJournalpostWebClient.post()
            .bodyValue(request)
            .retrieve()
            .bodyToMono(DistribuerJournalpostResponse::class.java)
            .block()
    }
}
