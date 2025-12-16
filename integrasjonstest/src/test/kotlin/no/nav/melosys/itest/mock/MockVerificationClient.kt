package no.nav.melosys.itest.mock

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter
import org.springframework.web.client.RestTemplate

/**
 * Client for calling verification endpoints on melosys-mock.
 *
 * This client provides methods to query what data was sent to the mock during tests,
 * enabling verification without direct in-process repo access.
 *
 * Usage:
 * ```kotlin
 * val client = MockVerificationClient(mockContainer.getBaseUrl())
 *
 * // Verify MEDL perioder were created
 * client.medl().shouldHaveSize(1)
 *
 * // Verify SEDs were sent
 * client.sedForRinaSak("123456").shouldContainInOrder("A012", "X008", "A004")
 *
 * // Clear all mock data between tests
 * client.clear()
 * ```
 */
class MockVerificationClient(
    private val baseUrl: String,
    private val restTemplate: RestTemplate = createRestTemplate()
) {
    companion object {
        private val log = LoggerFactory.getLogger(MockVerificationClient::class.java)
        private const val VERIFICATION_BASE_PATH = "/testdata/verification"

        private fun createRestTemplate(): RestTemplate {
            val objectMapper: ObjectMapper = jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

            return RestTemplate().apply {
                messageConverters.removeIf { it is MappingJackson2HttpMessageConverter }
                messageConverters.add(MappingJackson2HttpMessageConverter(objectMapper))
            }
        }
    }

    // ==================== MEDL ====================

    /**
     * Get all MEDL medlemskapsunntak stored in the mock.
     */
    fun medl(): List<MedlemskapsunntakVerificationDto> {
        return try {
            restTemplate.exchange(
                "$baseUrl$VERIFICATION_BASE_PATH/medl",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<List<MedlemskapsunntakVerificationDto>>() {}
            ).body ?: emptyList()
        } catch (e: Exception) {
            log.warn("Failed to fetch MEDL data from mock: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get count of MEDL medlemskapsunntak.
     */
    fun medlCount(): Int {
        return try {
            // Docker mock returns CountResponse(count), in-process mock may return Int directly
            val response = restTemplate.getForObject("$baseUrl$VERIFICATION_BASE_PATH/medl/count", CountResponse::class.java)
            response?.count ?: 0
        } catch (e: Exception) {
            log.warn("Failed to fetch MEDL count from mock: ${e.message}")
            0
        }
    }

    // ==================== SAK ====================

    /**
     * Get all saker stored in the mock.
     */
    fun saker(): List<SakVerificationDto> {
        return try {
            restTemplate.exchange(
                "$baseUrl$VERIFICATION_BASE_PATH/sak",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<List<SakVerificationDto>>() {}
            ).body ?: emptyList()
        } catch (e: Exception) {
            log.warn("Failed to fetch sak data from mock: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get count of saker.
     */
    fun sakCount(): Int {
        return try {
            val response = restTemplate.getForObject("$baseUrl$VERIFICATION_BASE_PATH/sak/count", CountResponse::class.java)
            response?.count ?: 0
        } catch (e: Exception) {
            log.warn("Failed to fetch sak count from mock: ${e.message}")
            0
        }
    }

    /**
     * Get sak by fagsakNr.
     */
    fun sakByFagsakNr(fagsakNr: String): SakVerificationDto? {
        return try {
            restTemplate.getForObject("$baseUrl$VERIFICATION_BASE_PATH/sak/fagsak/$fagsakNr", SakVerificationDto::class.java)
        } catch (e: Exception) {
            log.warn("Failed to fetch sak by fagsakNr $fagsakNr from mock: ${e.message}")
            null
        }
    }

    // ==================== MELOSYS-EESSI ====================

    /**
     * Get SED types sent for a specific RINA sak.
     * Returns list of SED type strings (e.g., ["A012", "X008", "A004"]).
     */
    fun sedForRinaSak(rinaSaksnummer: String): List<String> {
        return try {
            restTemplate.exchange(
                "$baseUrl$VERIFICATION_BASE_PATH/melosys-eessi/sed/$rinaSaksnummer",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<List<String>>() {}
            ).body ?: emptyList()
        } catch (e: Exception) {
            log.warn("Failed to fetch SED data for RINA sak $rinaSaksnummer from mock: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get all SED repo data (rinaSaksnummer -> list of SED types).
     */
    fun allSedRepo(): Map<String, List<String>> {
        return try {
            restTemplate.exchange(
                "$baseUrl$VERIFICATION_BASE_PATH/melosys-eessi/sed",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<Map<String, List<String>>>() {}
            ).body ?: emptyMap()
        } catch (e: Exception) {
            log.warn("Failed to fetch all SED repo data from mock: ${e.message}")
            emptyMap()
        }
    }

    /**
     * Get all BUC information stored in the mock.
     */
    fun bucInfo(): List<BucVerificationDto> {
        return try {
            restTemplate.exchange(
                "$baseUrl$VERIFICATION_BASE_PATH/melosys-eessi/buc",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<List<BucVerificationDto>>() {}
            ).body ?: emptyList()
        } catch (e: Exception) {
            log.warn("Failed to fetch BUC data from mock: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get all saksrelasjoner stored in the mock.
     */
    fun saksrelasjoner(): List<SaksrelasjonVerificationDto> {
        return try {
            restTemplate.exchange(
                "$baseUrl$VERIFICATION_BASE_PATH/melosys-eessi/saksrelasjoner",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<List<SaksrelasjonVerificationDto>>() {}
            ).body ?: emptyList()
        } catch (e: Exception) {
            log.warn("Failed to fetch saksrelasjoner from mock: ${e.message}")
            emptyList()
        }
    }

    // ==================== OPPGAVE ====================

    /**
     * Get all oppgaver stored in the mock.
     */
    fun oppgaver(): List<OppgaveVerificationDto> {
        return try {
            restTemplate.exchange(
                "$baseUrl$VERIFICATION_BASE_PATH/oppgave",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<List<OppgaveVerificationDto>>() {}
            ).body ?: emptyList()
        } catch (e: Exception) {
            log.warn("Failed to fetch oppgave data from mock: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get count of oppgaver.
     */
    fun oppgaveCount(): Int {
        return try {
            val response = restTemplate.getForObject("$baseUrl$VERIFICATION_BASE_PATH/oppgave/count", CountResponse::class.java)
            response?.count ?: 0
        } catch (e: Exception) {
            log.warn("Failed to fetch oppgave count from mock: ${e.message}")
            0
        }
    }

    /**
     * Get oppgaver by type (e.g., "JFR", "BEH_SED").
     */
    fun oppgaverByType(oppgavetype: String): List<OppgaveVerificationDto> {
        return try {
            restTemplate.exchange(
                "$baseUrl$VERIFICATION_BASE_PATH/oppgave/type/$oppgavetype",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<List<OppgaveVerificationDto>>() {}
            ).body ?: emptyList()
        } catch (e: Exception) {
            log.warn("Failed to fetch oppgaver by type $oppgavetype from mock: ${e.message}")
            emptyList()
        }
    }

    // ==================== JOURNALPOST ====================

    /**
     * Get all journalposter stored in the mock.
     */
    fun journalposter(): List<JournalpostVerificationDto> {
        return try {
            restTemplate.exchange(
                "$baseUrl$VERIFICATION_BASE_PATH/journalpost",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<List<JournalpostVerificationDto>>() {}
            ).body ?: emptyList()
        } catch (e: Exception) {
            log.warn("Failed to fetch journalpost data from mock: ${e.message}")
            emptyList()
        }
    }

    /**
     * Get count of journalposter.
     */
    fun journalpostCount(): Int {
        return try {
            val response = restTemplate.getForObject("$baseUrl$VERIFICATION_BASE_PATH/journalpost/count", CountResponse::class.java)
            response?.count ?: 0
        } catch (e: Exception) {
            log.warn("Failed to fetch journalpost count from mock: ${e.message}")
            0
        }
    }

    /**
     * Get journalpost by ID.
     */
    fun journalpostById(journalpostId: String): JournalpostVerificationDto? {
        return try {
            restTemplate.getForObject("$baseUrl$VERIFICATION_BASE_PATH/journalpost/$journalpostId", JournalpostVerificationDto::class.java)
        } catch (e: Exception) {
            log.warn("Failed to fetch journalpost by id $journalpostId from mock: ${e.message}")
            null
        }
    }

    /**
     * Get journalposter by saksnummer.
     */
    fun journalposterBySak(saksnummer: String): List<JournalpostVerificationDto> {
        return try {
            restTemplate.exchange(
                "$baseUrl$VERIFICATION_BASE_PATH/journalpost/sak/$saksnummer",
                HttpMethod.GET,
                null,
                object : ParameterizedTypeReference<List<JournalpostVerificationDto>>() {}
            ).body ?: emptyList()
        } catch (e: Exception) {
            log.warn("Failed to fetch journalposter by sak $saksnummer from mock: ${e.message}")
            emptyList()
        }
    }

    // ==================== SUMMARY ====================

    /**
     * Get summary of all mock data counts.
     */
    fun summary(): MockSummaryDto {
        return try {
            restTemplate.getForObject("$baseUrl$VERIFICATION_BASE_PATH/summary", MockSummaryDto::class.java)
                ?: MockSummaryDto()
        } catch (e: Exception) {
            log.warn("Failed to fetch summary from mock: ${e.message}")
            MockSummaryDto()
        }
    }

    // ==================== CLEAR ====================

    /**
     * Clear all mock data.
     * Should be called in @AfterEach to ensure test isolation.
     */
    fun clear(): ClearResponse {
        return try {
            restTemplate.exchange(
                "$baseUrl/testdata/clear",
                HttpMethod.DELETE,
                null,
                ClearResponse::class.java
            ).body ?: ClearResponse(message = "No response")
        } catch (e: Exception) {
            log.warn("Failed to clear mock data: ${e.message}")
            ClearResponse(message = "Error: ${e.message}")
        }
    }

    // ==================== TEST DATA CREATION ====================

    /**
     * Create a journalføringsoppgave in the mock.
     * Returns the created Oppgave with id and journalpostId set.
     *
     * @param tilordnetRessurs The saksbehandler to assign the oppgave to (default: "Z123456")
     * @param forVirksomhet Whether the oppgave is for a virksomhet (default: false)
     * @param medVedlegg Whether to include vedlegg (default: false)
     * @param medLogiskVedlegg Whether to include logisk vedlegg (default: false)
     * @return The created Oppgave
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

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        val response = restTemplate.exchange(
            "$baseUrl/testdata/jfr-oppgave",
            HttpMethod.POST,
            HttpEntity(request, headers),
            object : ParameterizedTypeReference<List<OppgaveVerificationDto>>() {}
        )

        return response.body?.firstOrNull()
            ?: throw IllegalStateException("Failed to create jfr-oppgave: no response from mock")
    }

    /**
     * Create BUC info in the mock for test setup.
     * This is used to set up test data before running tests that expect BUC info to exist.
     *
     * @param id The BUC ID (RINA saksnummer)
     * @param erAapen Whether the BUC is open (default: true)
     * @param bucType The BUC type (e.g., "LA_BUC_04")
     * @param opprettetDato When the BUC was created
     * @param mottakerinstitusjoner Set of receiver institutions
     * @param seder List of SED info
     * @return The created BUC verification DTO
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

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        val response = restTemplate.exchange(
            "$baseUrl/testdata/buc",
            HttpMethod.POST,
            HttpEntity(request, headers),
            BucVerificationDto::class.java
        )

        return response.body
            ?: throw IllegalStateException("Failed to create BUC info: no response from mock")
    }

    /**
     * Create MEDL (medlemskapsunntak) test data in the mock.
     * This is used to set up pre-existing MEDL periods for tests.
     *
     * @param unntakId The specific ID to use (required, not auto-generated)
     * @param ident Person's fødselsnummer (required)
     * @param fraOgMed Start date (default: today)
     * @param tilOgMed End date (default: today + 1 year)
     * @param status Status (default: "GODKJENT")
     * @param dekning Coverage type (default: "FULL")
     * @param lovvalgsland Country code (default: "NO")
     * @param lovvalg Law choice (default: "FOROVRIG")
     * @param grunnlag Basis (default: "ARBEID")
     * @param medlem Member flag (default: true)
     * @return The created MedlemskapsunntakVerificationDto
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

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
        }

        val response = restTemplate.exchange(
            "$baseUrl/testdata/medl",
            HttpMethod.POST,
            HttpEntity(request, headers),
            MedlemskapsunntakVerificationDto::class.java
        )

        return response.body
            ?: throw IllegalStateException("Failed to create MEDL data: no response from mock")
    }

    // ==================== HEALTH ====================

    /**
     * Check if the mock is healthy and reachable.
     */
    fun isHealthy(): Boolean {
        return try {
            val response = restTemplate.getForObject("$baseUrl/actuator/health", Map::class.java)
            response?.get("status") == "UP"
        } catch (e: Exception) {
            log.warn("Mock health check failed: ${e.message}")
            false
        }
    }
}
