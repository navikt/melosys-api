package no.nav.melosys.itest.mock

import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter
import org.springframework.web.client.RestClient

/**
 * Klient for å kalle verifikasjonsendepunkter på melosys-mock.
 *
 * Denne klienten tilbyr metoder for å spørre hvilke data som ble sendt til mocken under tester,
 * noe som muliggjør verifisering uten direkte in-process repo-tilgang.
 *
 * Bruk:
 * ```kotlin
 * val client = MockVerificationClient(mockContainer.getBaseUrl())
 *
 * // Verifiser at MEDL-perioder ble opprettet
 * client.medl().shouldHaveSize(1)
 *
 * // Verifiser at SEDer ble sendt
 * client.sedForRinaSak("123456").shouldContainInOrder("A012", "X008", "A004")
 *
 * // Tøm alle mock-data mellom tester
 * client.clear()
 * ```
 *
 * @param baseUrl Base-URL for mock-containeren
 * @param strictMode Når true, kastes unntak ved kommunikasjonsfeil i stedet for å returnere tomme resultater.
 *                   Standard: true. Sett MOCK_VERIFICATION_STRICT_MODE=false for å deaktivere.
 * @param restClient RestClient-instans for HTTP-kall
 */
class MockVerificationClient(
    private val baseUrl: String,
    private val strictMode: Boolean = System.getenv("MOCK_VERIFICATION_STRICT_MODE")?.toBoolean() ?: true,
    private val restClient: RestClient = createRestClient()
) {
    companion object {
        private val log = LoggerFactory.getLogger(MockVerificationClient::class.java)
        private const val VERIFICATION_BASE_PATH = "/testdata/verification"

        private fun createRestClient(): RestClient {
            val objectMapper = JsonMapper.builder()
                .addModule(kotlinModule())
                .disable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build()

            return RestClient.builder()
                .messageConverters { converters ->
                    converters.removeIf { it is JacksonJsonHttpMessageConverter }
                    converters.add(JacksonJsonHttpMessageConverter(objectMapper))
                }
                .build()
        }
    }

    /**
     * Håndterer feil basert på strictMode-innstillingen.
     * I strict mode kastes unntaket på nytt, ellers logges en advarsel og standardverdien returneres.
     */
    private fun <T> handleError(operation: String, e: Exception, defaultValue: T): T {
        if (strictMode) {
            throw MockVerificationException("$operation feilet: ${e.message}", e)
        }
        log.warn("$operation: ${e.message}")
        return defaultValue
    }

    // ==================== MEDL ====================

    /**
     * Henter alle MEDL-medlemskapsunntak lagret i mocken.
     */
    fun medl(): List<MedlemskapsunntakVerificationDto> {
        return try {
            restClient.get()
                .uri("$baseUrl$VERIFICATION_BASE_PATH/medl")
                .retrieve()
                .body(object : ParameterizedTypeReference<List<MedlemskapsunntakVerificationDto>>() {})
                ?: emptyList()
        } catch (e: Exception) {
            handleError("Henting av MEDL-data fra mock", e, emptyList())
        }
    }

    /**
     * Henter antall MEDL-medlemskapsunntak.
     */
    fun medlCount(): Int {
        return try {
            restClient.get()
                .uri("$baseUrl$VERIFICATION_BASE_PATH/medl/count")
                .retrieve()
                .body(CountResponse::class.java)
                ?.count ?: 0
        } catch (e: Exception) {
            handleError("Henting av MEDL-antall fra mock", e, 0)
        }
    }

    // ==================== SAK ====================

    /**
     * Henter alle saker lagret i mocken.
     */
    fun saker(): List<SakVerificationDto> {
        return try {
            restClient.get()
                .uri("$baseUrl$VERIFICATION_BASE_PATH/sak")
                .retrieve()
                .body(object : ParameterizedTypeReference<List<SakVerificationDto>>() {})
                ?: emptyList()
        } catch (e: Exception) {
            handleError("Henting av sak-data fra mock", e, emptyList())
        }
    }

    /**
     * Henter antall saker.
     */
    fun sakCount(): Int {
        return try {
            restClient.get()
                .uri("$baseUrl$VERIFICATION_BASE_PATH/sak/count")
                .retrieve()
                .body(CountResponse::class.java)
                ?.count ?: 0
        } catch (e: Exception) {
            handleError("Henting av sak-antall fra mock", e, 0)
        }
    }

    /**
     * Henter sak etter fagsakNr.
     */
    fun sakByFagsakNr(fagsakNr: String): SakVerificationDto? {
        return try {
            restClient.get()
                .uri("$baseUrl$VERIFICATION_BASE_PATH/sak/fagsak/$fagsakNr")
                .retrieve()
                .body(SakVerificationDto::class.java)
        } catch (e: Exception) {
            handleError("Henting av sak med fagsakNr $fagsakNr fra mock", e, null)
        }
    }

    // ==================== MELOSYS-EESSI ====================

    /**
     * Henter SED-typer sendt for en spesifikk RINA-sak.
     * Returnerer liste med SED-typestrenger (f.eks. ["A012", "X008", "A004"]).
     */
    fun sedForRinaSak(rinaSaksnummer: String): List<String> {
        return try {
            restClient.get()
                .uri("$baseUrl$VERIFICATION_BASE_PATH/melosys-eessi/sed/$rinaSaksnummer")
                .retrieve()
                .body(object : ParameterizedTypeReference<List<String>>() {})
                ?: emptyList()
        } catch (e: Exception) {
            handleError("Henting av SED-data for RINA-sak $rinaSaksnummer fra mock", e, emptyList())
        }
    }

    /**
     * Henter alle SED-repo-data (rinaSaksnummer -> liste med SED-typer).
     */
    fun allSedRepo(): Map<String, List<String>> {
        return try {
            restClient.get()
                .uri("$baseUrl$VERIFICATION_BASE_PATH/melosys-eessi/sed")
                .retrieve()
                .body(object : ParameterizedTypeReference<Map<String, List<String>>>() {})
                ?: emptyMap()
        } catch (e: Exception) {
            handleError("Henting av alle SED-repo-data fra mock", e, emptyMap())
        }
    }

    /**
     * Henter all BUC-informasjon lagret i mocken.
     */
    fun bucInfo(): List<BucVerificationDto> {
        return try {
            restClient.get()
                .uri("$baseUrl$VERIFICATION_BASE_PATH/melosys-eessi/buc")
                .retrieve()
                .body(object : ParameterizedTypeReference<List<BucVerificationDto>>() {})
                ?: emptyList()
        } catch (e: Exception) {
            handleError("Henting av BUC-data fra mock", e, emptyList())
        }
    }

    /**
     * Henter alle saksrelasjoner lagret i mocken.
     */
    fun saksrelasjoner(): List<SaksrelasjonVerificationDto> {
        return try {
            restClient.get()
                .uri("$baseUrl$VERIFICATION_BASE_PATH/melosys-eessi/saksrelasjoner")
                .retrieve()
                .body(object : ParameterizedTypeReference<List<SaksrelasjonVerificationDto>>() {})
                ?: emptyList()
        } catch (e: Exception) {
            handleError("Henting av saksrelasjoner fra mock", e, emptyList())
        }
    }

    // ==================== OPPGAVE ====================

    /**
     * Henter alle oppgaver lagret i mocken.
     */
    fun oppgaver(): List<OppgaveVerificationDto> {
        return try {
            restClient.get()
                .uri("$baseUrl$VERIFICATION_BASE_PATH/oppgave")
                .retrieve()
                .body(object : ParameterizedTypeReference<List<OppgaveVerificationDto>>() {})
                ?: emptyList()
        } catch (e: Exception) {
            handleError("Henting av oppgave-data fra mock", e, emptyList())
        }
    }

    /**
     * Henter antall oppgaver.
     */
    fun oppgaveCount(): Int {
        return try {
            restClient.get()
                .uri("$baseUrl$VERIFICATION_BASE_PATH/oppgave/count")
                .retrieve()
                .body(CountResponse::class.java)
                ?.count ?: 0
        } catch (e: Exception) {
            handleError("Henting av oppgave-antall fra mock", e, 0)
        }
    }

    /**
     * Henter oppgaver etter type (f.eks. "JFR", "BEH_SED").
     */
    fun oppgaverByType(oppgavetype: String): List<OppgaveVerificationDto> {
        return try {
            restClient.get()
                .uri("$baseUrl$VERIFICATION_BASE_PATH/oppgave/type/$oppgavetype")
                .retrieve()
                .body(object : ParameterizedTypeReference<List<OppgaveVerificationDto>>() {})
                ?: emptyList()
        } catch (e: Exception) {
            handleError("Henting av oppgaver med type $oppgavetype fra mock", e, emptyList())
        }
    }

    // ==================== JOURNALPOST ====================

    /**
     * Henter alle journalposter lagret i mocken.
     */
    fun journalposter(): List<JournalpostVerificationDto> {
        return try {
            restClient.get()
                .uri("$baseUrl$VERIFICATION_BASE_PATH/journalpost")
                .retrieve()
                .body(object : ParameterizedTypeReference<List<JournalpostVerificationDto>>() {})
                ?: emptyList()
        } catch (e: Exception) {
            handleError("Henting av journalpost-data fra mock", e, emptyList())
        }
    }

    /**
     * Henter antall journalposter.
     */
    fun journalpostCount(): Int {
        return try {
            restClient.get()
                .uri("$baseUrl$VERIFICATION_BASE_PATH/journalpost/count")
                .retrieve()
                .body(CountResponse::class.java)
                ?.count ?: 0
        } catch (e: Exception) {
            handleError("Henting av journalpost-antall fra mock", e, 0)
        }
    }

    /**
     * Henter journalpost etter ID.
     */
    fun journalpostById(journalpostId: String): JournalpostVerificationDto? {
        return try {
            restClient.get()
                .uri("$baseUrl$VERIFICATION_BASE_PATH/journalpost/$journalpostId")
                .retrieve()
                .body(JournalpostVerificationDto::class.java)
        } catch (e: Exception) {
            handleError("Henting av journalpost med id $journalpostId fra mock", e, null)
        }
    }

    /**
     * Henter journalposter etter saksnummer.
     */
    fun journalposterBySak(saksnummer: String): List<JournalpostVerificationDto> {
        return try {
            restClient.get()
                .uri("$baseUrl$VERIFICATION_BASE_PATH/journalpost/sak/$saksnummer")
                .retrieve()
                .body(object : ParameterizedTypeReference<List<JournalpostVerificationDto>>() {})
                ?: emptyList()
        } catch (e: Exception) {
            handleError("Henting av journalposter for sak $saksnummer fra mock", e, emptyList())
        }
    }

    // ==================== OPPSUMMERING ====================

    /**
     * Henter oppsummering av alle mock-data-antall.
     */
    fun summary(): MockSummaryDto {
        return try {
            restClient.get()
                .uri("$baseUrl$VERIFICATION_BASE_PATH/summary")
                .retrieve()
                .body(MockSummaryDto::class.java)
                ?: MockSummaryDto()
        } catch (e: Exception) {
            handleError("Henting av oppsummering fra mock", e, MockSummaryDto())
        }
    }

    // ==================== TØMMING ====================

    /**
     * Tømmer alle mock-data.
     * Bør kalles i @AfterEach for å sikre testisolasjon.
     */
    fun clear(): ClearResponse {
        return try {
            restClient.delete()
                .uri("$baseUrl/testdata/clear")
                .retrieve()
                .body(ClearResponse::class.java)
                ?: ClearResponse(message = "No response")
        } catch (e: Exception) {
            handleError("Tømming av mock-data", e, ClearResponse(message = "Error: ${e.message}"))
        }
    }

    // ==================== TESTDATA-OPPRETTELSE ====================

    /**
     * Oppretter en journalføringsoppgave i mocken.
     * Returnerer den opprettede oppgaven med id og journalpostId satt.
     *
     * @param tilordnetRessurs Saksbehandleren oppgaven skal tilordnes (standard: "Z123456")
     * @param forVirksomhet Om oppgaven er for en virksomhet (standard: false)
     * @param medVedlegg Om vedlegg skal inkluderes (standard: false)
     * @param medLogiskVedlegg Om logisk vedlegg skal inkluderes (standard: false)
     * @return Den opprettede oppgaven
     */
    fun opprettJfrOppgave(
        tilordnetRessurs: String = "Z123456",
        forVirksomhet: Boolean = false,
        medVedlegg: Boolean = false,
        medLogiskVedlegg: Boolean = false
    ): OppgaveVerificationDto {
        val request = OpprettJfrOppgaveRequest(
            antall = 1,
            tilordnetRessurs = tilordnetRessurs,
            forVirksomhet = forVirksomhet,
            medVedlegg = medVedlegg,
            medLogiskVedlegg = medLogiskVedlegg
        )

        val response = restClient.post()
            .uri("$baseUrl/testdata/jfr-oppgave")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(object : ParameterizedTypeReference<List<OppgaveVerificationDto>>() {})

        return response?.firstOrNull()
            ?: throw IllegalStateException("Failed to create jfr-oppgave: no response from mock")
    }

    /**
     * Oppretter BUC-info i mocken for testoppsett.
     * Brukes for å sette opp testdata før kjøring av tester som forventer at BUC-info eksisterer.
     *
     * @param id BUC-ID (RINA-saksnummer)
     * @param erAapen Om BUCen er åpen (standard: true)
     * @param bucType BUC-type (f.eks. "LA_BUC_04")
     * @param opprettetDato Når BUCen ble opprettet
     * @param mottakerinstitusjoner Sett med mottakerinstitusjoner
     * @param seder Liste med SED-info
     * @return Den opprettede BUC-verifikasjons-DTOen
     */
    fun opprettBucinformasjon(
        id: String?,
        erAapen: Boolean = true,
        bucType: String? = null,
        opprettetDato: java.time.LocalDate? = null,
        mottakerinstitusjoner: Set<String>? = null,
        seder: List<OpprettSedRequest>? = null
    ): BucVerificationDto {
        val request = OpprettBucRequest(
            id = id,
            erAapen = erAapen,
            bucType = bucType,
            opprettetDato = opprettetDato,
            mottakerinstitusjoner = mottakerinstitusjoner,
            seder = seder
        )

        val response = restClient.post()
            .uri("$baseUrl/testdata/buc")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(BucVerificationDto::class.java)

        return response
            ?: throw IllegalStateException("Failed to create BUC info: no response from mock")
    }

    /**
     * Oppretter MEDL (medlemskapsunntak) testdata i mocken.
     * Brukes for å sette opp eksisterende MEDL-perioder for tester.
     *
     * @param unntakId Den spesifikke IDen som skal brukes (påkrevd, ikke auto-generert)
     * @param ident Personens fødselsnummer (påkrevd)
     * @param fraOgMed Startdato (standard: i dag)
     * @param tilOgMed Sluttdato (standard: i dag + 1 år)
     * @param status Status (standard: "GODKJENT")
     * @param dekning Dekningstype (standard: "FULL")
     * @param lovvalgsland Landkode (standard: "NO")
     * @param lovvalg Lovvalg (standard: "FOROVRIG")
     * @param grunnlag Grunnlag (standard: "ARBEID")
     * @param medlem Medlemsflagg (standard: true)
     * @return Den opprettede MedlemskapsunntakVerificationDto
     */
    fun opprettMedl(
        unntakId: Long,
        ident: String,
        fraOgMed: java.time.LocalDate? = null,
        tilOgMed: java.time.LocalDate? = null,
        status: String? = null,
        dekning: String? = null,
        lovvalgsland: String? = null,
        lovvalg: String? = null,
        grunnlag: String? = null,
        medlem: Boolean? = null
    ): MedlemskapsunntakVerificationDto {
        val request = OpprettMedlRequest(
            unntakId = unntakId,
            ident = ident,
            fraOgMed = fraOgMed,
            tilOgMed = tilOgMed,
            status = status,
            dekning = dekning,
            lovvalgsland = lovvalgsland,
            lovvalg = lovvalg,
            grunnlag = grunnlag,
            medlem = medlem
        )

        val response = restClient.post()
            .uri("$baseUrl/testdata/medl")
            .contentType(MediaType.APPLICATION_JSON)
            .body(request)
            .retrieve()
            .body(MedlemskapsunntakVerificationDto::class.java)

        return response
            ?: throw IllegalStateException("Failed to create MEDL data: no response from mock")
    }

    // ==================== HELSE ====================

    /**
     * Sjekker om mocken er sunn og tilgjengelig.
     */
    fun isHealthy(): Boolean {
        return try {
            val response = restClient.get()
                .uri("$baseUrl/actuator/health")
                .retrieve()
                .body(object : ParameterizedTypeReference<Map<String, Any>>() {})
            response?.get("status") == "UP"
        } catch (e: Exception) {
            handleError("Helsesjekk av mock", e, false)
        }
    }
}

/**
 * Unntak som kastes når MockVerificationClient er i strict mode og en operasjon feiler.
 * Brukes for å fange opp kommunikasjonsproblemer med mock-containeren i CI-miljøer.
 */
class MockVerificationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
