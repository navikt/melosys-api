package no.nav.melosys.integrasjon.joark.journalpostapi

import mu.KotlinLogging
import no.nav.melosys.exception.TekniskException
import no.nav.melosys.integrasjon.felles.JsonRestIntegrasjon
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.FerdigstillJournalpostRequest
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.LogiskVedleggRequest
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OppdaterJournalpostRequest
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostRequest
import no.nav.melosys.integrasjon.joark.journalpostapi.dto.OpprettJournalpostResponse
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.ObjectMapper

private val log = KotlinLogging.logger { }

@Service
class JournalpostapiClient(
    private val journalpostapiWebClient: WebClient,
    private val objectMapper: ObjectMapper
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

    /**
     * Som [opprettJournalpost], men en 409 (journalposten finnes allerede) returnerer den eksisterende
     * journalposten i stedet for å kaste feil.
     */
    fun opprettJournalpostIdempotent(
        request: OpprettJournalpostRequest,
        forsøkEndeligJfr: Boolean
    ): OpprettJournalpostResponse =
        try {
            opprettJournalpost(request, forsøkEndeligJfr)
        } catch (e: JournalpostConflictException) {
            objectMapper.readValue(e.responseBody, OpprettJournalpostResponse::class.java)
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

/**
 * Joark svarer 409 når en journalpost med samme eksternReferanseId allerede finnes.
 * Responsbodyen inneholder den eksisterende journalposten (inkludert journalpostId).
 */
class JournalpostConflictException(val responseBody: String) :
    TekniskException("Journalpost finnes allerede i Joark (409 CONFLICT)")

fun lagJournalpostApiException(feilmelding: String, statusCode: HttpStatusCode, errorBody: String): Exception =
    if (statusCode == HttpStatus.CONFLICT) {
        JournalpostConflictException(errorBody)
    } else {
        TekniskException("$feilmelding $statusCode - $errorBody")
    }

