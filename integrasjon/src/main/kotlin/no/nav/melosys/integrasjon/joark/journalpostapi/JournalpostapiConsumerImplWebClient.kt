package no.nav.melosys.integrasjon.joark.journalpostapi

import no.nav.melosys.integrasjon.felles.JsonRestIntegrasjon
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.FerdigstillJournalpostRequest
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.LogiskVedleggRequest
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OppdaterJournalpostRequest
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostRequest
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostResponse
import org.slf4j.LoggerFactory
import org.springframework.web.reactive.function.client.WebClient

open class JournalpostapiConsumerImplWebClient(
    private val webClient: WebClient
) : JournalpostapiConsumer, JsonRestIntegrasjon {

    private val log = LoggerFactory.getLogger(JournalpostapiConsumerImpl::class.java)

    override fun opprettJournalpost(
        request: OpprettJournalpostRequest,
        forsøkEndeligJfr: Boolean
    ): OpprettJournalpostResponse? {
        if (log.isInfoEnabled) {
            log.info(
                "Oppretter journalpost av type {} for sak {}",
                request.journalpostType.name,
                request.sak?.fagsakId ?: "ukjent"
            )
        }

        return webClient.post()
            .uri("/journalpost?forsoekFerdigstill={forsoekFerdigstill}", forsøkEndeligJfr)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(OpprettJournalpostResponse::class.java)
            .block()
    }

    override fun oppdaterJournalpost(request: OppdaterJournalpostRequest, journalpostId: String) {
        if (log.isInfoEnabled) {
            log.info("Oppdaterer journalpost med id {}", journalpostId)
        }

        webClient.put()
            .uri("/journalpost/{journalpostID}", journalpostId)
            .bodyValue(request)
            .retrieve()
            .toBodilessEntity()
            .block()
    }

    override fun leggTilLogiskVedlegg(dokumentInfoId: String, tittel: String) {
        if (log.isInfoEnabled) {
            log.info("Legger til logisk vedlegg for dokument med id {}", dokumentInfoId)
        }

        val logiskVedleggRequest = LogiskVedleggRequest(tittel)

        webClient.post()
            .uri("/dokumentInfo/{dokumentInfoId}/logiskVedlegg/", dokumentInfoId)
            .bodyValue(logiskVedleggRequest)
            .retrieve()
            .toBodilessEntity()
            .block()
    }

    override fun fjernLogiskeVedlegg(dokumentInfoId: String, logiskVedleggId: String) {
        if (log.isInfoEnabled) {
            log.info("Fjerner logisk vedlegg {} for dokument med id {}", logiskVedleggId, dokumentInfoId)
        }

        webClient.delete()
            .uri("/dokumentInfo/{dokumentInfoId}/logiskVedlegg/{logiskVedleggId}", dokumentInfoId, logiskVedleggId)
            .retrieve()
            .toBodilessEntity()
            .block()
    }

    override fun ferdigstillJournalpost(request: FerdigstillJournalpostRequest, journalpostId: String) {
        if (log.isInfoEnabled) {
            log.info("Ferdigstill journalpost med id {}", journalpostId)
        }

        webClient.patch()
            .uri("/journalpost/{journalpostID}/ferdigstill", journalpostId)
            .bodyValue(request)
            .retrieve()
            .toBodilessEntity()
            .block()
    }
}
