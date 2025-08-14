package no.nav.melosys.integrasjon.doksys.distribuerjournalpost

import no.nav.melosys.exception.IntegrasjonException
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostRequest
import no.nav.melosys.integrasjon.doksys.distribuerjournalpost.dto.DistribuerJournalpostResponse
import no.nav.melosys.integrasjon.felles.RestErrorHandler
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.retry.annotation.Retryable
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException

@Retryable
open class DistribuerJournalpostConsumerWebClientImpl(
    private val webClient: WebClient
) : RestErrorHandler(), DistribuerJournalpostConsumer {

    private val log = LoggerFactory.getLogger(DistribuerJournalpostConsumerWebClientImpl::class.java)

    override fun distribuerJournalpost(request: DistribuerJournalpostRequest): DistribuerJournalpostResponse? {
        log.info("Distribuerer journalpost {}", request.journalpostId)

        val headers = HttpHeaders().apply {
            add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            add(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        }

        return webClient.post()
            .headers { httpHeaders -> httpHeaders.addAll(headers) }
            .bodyValue(request)
            .retrieve()
            .bodyToMono(DistribuerJournalpostResponse::class.java)
            .doOnError(WebClientResponseException::class.java) { webClientResponseException ->
                throw tilException(
                    webClientResponseException.responseBodyAsString,
                    webClientResponseException.statusCode
                )
            }
            .doOnError { ex ->
                throw IntegrasjonException("Ukjent feil mot distribuerjournalpost", ex)
            }
            .block()
    }
}
