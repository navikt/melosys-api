package no.nav.melosys.integrasjon.joark.journalpostapi

import mu.KotlinLogging
import no.nav.melosys.integrasjon.felles.JsonRestIntegrasjon
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.FerdigstillJournalpostRequest
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.LogiskVedleggRequest
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OppdaterJournalpostRequest
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostRequest
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostResponse
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

private val log = KotlinLogging.logger { }

@Service
class JournalpostapiConsumer(
    private val journalpostapiWebClient: WebClient
) : JsonRestIntegrasjon {

    fun opprettJournalpost(
        request: OpprettJournalpostRequest,
        forsøkEndeligJfr: Boolean
    ): OpprettJournalpostResponse {
        if (log.isInfoEnabled) {
            log.info(
                "Oppretter journalpost av type {} for sak {}",
                request.journalpostType.name,
                request.sak?.fagsakId ?: "ukjent"
            )
        }

        return journalpostapiWebClient.post()
            .uri("/journalpost?forsoekFerdigstill={forsoekFerdigstill}", forsøkEndeligJfr)
            .bodyValue(request)
            .retrieve()
            .bodyToMono(OpprettJournalpostResponse::class.java)
            .block() ?: error("Kunne ikke hente body for POST /journalpost")
    }

    fun oppdaterJournalpost(request: OppdaterJournalpostRequest, journalpostId: String) {
        if (log.isInfoEnabled) {
            log.info("Oppdaterer journalpost med id {}", journalpostId)
        }

        journalpostapiWebClient.put()
            .uri("/journalpost/{journalpostID}", journalpostId)
            .bodyValue(request)
            .retrieve()
            .toBodilessEntity()
            .block()
    }

    fun leggTilLogiskVedlegg(dokumentInfoId: String, tittel: String) {
        if (log.isInfoEnabled) {
            log.info("Legger til logisk vedlegg for dokument med id {}", dokumentInfoId)
        }

        val logiskVedleggRequest = LogiskVedleggRequest(tittel)

        journalpostapiWebClient.post()
            .uri("/dokumentInfo/{dokumentInfoId}/logiskVedlegg/", dokumentInfoId)
            .bodyValue(logiskVedleggRequest)
            .retrieve()
            .toBodilessEntity()
            .block()
    }

    fun fjernLogiskeVedlegg(dokumentInfoId: String, logiskVedleggId: String) {
        if (log.isInfoEnabled) {
            log.info("Fjerner logisk vedlegg {} for dokument med id {}", logiskVedleggId, dokumentInfoId)
        }

        journalpostapiWebClient.delete()
            .uri("/dokumentInfo/{dokumentInfoId}/logiskVedlegg/{logiskVedleggId}", dokumentInfoId, logiskVedleggId)
            .retrieve()
            .toBodilessEntity()
            .block()
    }

    fun ferdigstillJournalpost(request: FerdigstillJournalpostRequest, journalpostId: String) {
        if (log.isInfoEnabled) {
            log.info("Ferdigstill journalpost med id {}", journalpostId)
        }

        journalpostapiWebClient.patch()
            .uri("/journalpost/{journalpostID}/ferdigstill", journalpostId)
            .bodyValue(request)
            .retrieve()
            .toBodilessEntity()
            .block()
    }
}
