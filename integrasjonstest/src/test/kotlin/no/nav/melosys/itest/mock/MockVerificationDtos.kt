package no.nav.melosys.itest.mock

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

/**
 * DTOs for verification endpoint responses from melosys-mock.
 * These match the structure returned by /testdata/verification/* endpoints.
 */

/**
 * DTO for MEDL medlemskapsunntak.
 * Matches the structure of MedlemskapsunntakForGet from the MEDL API.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class MedlemskapsunntakVerificationDto(
    val unntakId: Long? = null,
    val ident: String? = null,
    val fraOgMed: LocalDate? = null,
    val tilOgMed: LocalDate? = null,
    val status: String? = null,
    val dekning: String? = null,
    val lovvalgsland: String? = null,
    val lovvalg: String? = null,
    val grunnlag: String? = null,
    val medlem: Boolean? = null,
    val sporingsinformasjon: SporingsinformasjonDto? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SporingsinformasjonDto(
    val versjon: Int? = null,
    val registrert: LocalDate? = null,
    val besluttet: LocalDate? = null,
    val kilde: String? = null,
    val kildedokument: String? = null,
    val opprettet: LocalDateTime? = null,
    val opprettetAv: String? = null,
    val sistEndret: LocalDateTime? = null,
    val sistEndretAv: String? = null
)

/**
 * DTO for Sak (arkivsak).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SakVerificationDto(
    val id: Long? = null,
    val tema: String? = null,
    val applikasjon: String? = null,
    val fagsakNr: String? = null,
    val aktoerId: String? = null,
    val orgnr: String? = null
)

/**
 * DTO for Oppgave.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class OppgaveVerificationDto(
    val id: Int? = null,
    val aktivDato: LocalDate? = null,
    val aktoerId: String? = null,
    val behandlesAvApplikasjon: String? = null,
    val behandlingstema: String? = null,
    val behandlingstype: String? = null,
    val beskrivelse: String? = null,
    val endretTidspunkt: LocalDateTime? = null,
    val ferdigstiltTidspunkt: LocalDateTime? = null,
    val fristFerdigstillelse: LocalDate? = null,
    val journalpostId: String? = null,
    val oppgavetype: String? = null,
    val opprettetTidspunkt: ZonedDateTime? = null,
    val orgnr: String? = null,
    val prioritet: String? = null,
    val saksreferanse: String? = null,
    val status: String? = null,
    val tema: String? = null,
    val tildeltEnhetsnr: String? = null,
    val tilordnetRessurs: String? = null,
    val versjon: Int? = null,
    val metadata: Map<String, String>? = null
)

/**
 * DTO for Journalpost.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class JournalpostVerificationDto(
    val journalpostId: String? = null,
    val journalStatus: String? = null,
    val journalposttype: String? = null,
    val tittel: String? = null,
    val arkivtema: String? = null,
    val kanal: String? = null,
    val mottattDato: LocalDate? = null
)

/**
 * Response wrapper for count endpoints.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CountResponse(
    val count: Int = 0
)

/**
 * Response for clear endpoint.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class ClearResponse(
    val message: String? = null,
    val journalpostCleared: String? = null,
    val oppgaveCleared: String? = null,
    val medlCleared: String? = null,
    val sakCleared: String? = null,
    val melosysEessiCleared: String? = null
)
