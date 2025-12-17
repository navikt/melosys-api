package no.nav.melosys.itest

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

/**
 * DTOer for verifikasjonsendepunkt-responser fra melosys-mock.
 * Disse matcher strukturen returnert av /testdata/verification/ endepunktene.
 */

/**
 * DTO for MEDL-medlemskapsunntak.
 * Matcher strukturen til MedlemskapsunntakForGet fra MEDL API.
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
 * DTO for sak (arkivsak).
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
 * DTO for oppgave.
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
 * DTO for journalpost.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class JournalpostVerificationDto(
    val journalpostId: String? = null,
    val journalStatus: String? = null,
    val journalposttype: String? = null,
    val tittel: String? = null,
    val arkivtema: String? = null,
    val kanal: String? = null,
    val mottattDato: LocalDate? = null,
    val sakId: String? = null,
    val avsenderMottaker: AvsenderMottakerDto? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class AvsenderMottakerDto(
    val id: String? = null,
    val navn: String? = null,
    val type: String? = null
)

/**
 * Respons-wrapper for antall-endepunkter.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class CountResponse(
    val count: Int = 0
)

/**
 * Respons for tømme-endepunkt.
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

/**
 * DTO for BUC-informasjon fra melosys-eessi.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class BucVerificationDto(
    val id: String? = null,
    val erAapen: Boolean? = null,
    val bucType: String? = null,
    val opprettetDato: LocalDate? = null,
    val mottakerinstitusjoner: Set<String>? = null,
    val seder: List<SedVerificationDto>? = null
)

/**
 * DTO for SED-informasjon innenfor en BUC.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SedVerificationDto(
    val bucId: String? = null,
    val sedId: String? = null,
    val opprettetDato: LocalDate? = null,
    val sistOppdatert: LocalDate? = null,
    val sedType: String? = null,
    val status: String? = null,
    val rinaUrl: String? = null
)

/**
 * DTO for saksrelasjon mellom GSAK og RINA.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class SaksrelasjonVerificationDto(
    val gsakSaksnummer: Long? = null,
    val rinaSaksnummer: String? = null,
    val bucType: String? = null
)

/**
 * DTO for oppsummering av alle mock-data.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class MockSummaryDto(
    val medlCount: Int = 0,
    val sakCount: Int = 0,
    val oppgaveCount: Int = 0,
    val journalpostCount: Int = 0,
    val bucCount: Int = 0,
    val sedRepoCount: Int = 0,
    val saksrelasjonCount: Int = 0
)

// ==================== TESTDATA-OPPRETTELSE ====================

/**
 * Forespørsel for å opprette journalføringsoppgave(r) i mocken.
 */
data class OpprettJfrOppgaveRequest(
    val antall: Int = 1,
    val tilordnetRessurs: String = "Z123456",
    val forVirksomhet: Boolean = false,
    val medVedlegg: Boolean = false,
    val medLogiskVedlegg: Boolean = false
)

/**
 * Forespørsel for å opprette BUC-info i mocken.
 * Brukes for å sette opp testdata før kjøring av tester som forventer at BUC-info eksisterer.
 */
data class OpprettBucRequest(
    val id: String?,
    val erAapen: Boolean? = true,
    val bucType: String? = null,
    val opprettetDato: LocalDate? = null,
    val mottakerinstitusjoner: Set<String>? = null,
    val seder: List<OpprettSedRequest>? = null
)

/**
 * Forespørsel for å opprette SED-info innenfor en BUC.
 */
data class OpprettSedRequest(
    val bucId: String? = null,
    val sedId: String? = null,
    val opprettetDato: LocalDate? = null,
    val sistOppdatert: LocalDate? = null,
    val sedType: String? = null,
    val status: String? = null,
    val rinaUrl: String? = null
)

/**
 * Forespørsel for å opprette MEDL (medlemskapsunntak) testdata med en spesifikk unntakId.
 * Brukes for å sette opp eksisterende MEDL-perioder for tester.
 */
data class OpprettMedlRequest(
    val unntakId: Long,
    val ident: String,
    val fraOgMed: LocalDate? = null,
    val tilOgMed: LocalDate? = null,
    val status: String? = null,
    val dekning: String? = null,
    val lovvalgsland: String? = null,
    val lovvalg: String? = null,
    val grunnlag: String? = null,
    val medlem: Boolean? = null
)
