package no.nav.melosys.itest.mock

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpMethod
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
            restTemplate.getForObject("$baseUrl$VERIFICATION_BASE_PATH/medl/count", Int::class.java) ?: 0
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
            restTemplate.getForObject("$baseUrl$VERIFICATION_BASE_PATH/sak/count", Int::class.java) ?: 0
        } catch (e: Exception) {
            log.warn("Failed to fetch sak count from mock: ${e.message}")
            0
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
            restTemplate.getForObject("$baseUrl$VERIFICATION_BASE_PATH/oppgave/count", Int::class.java) ?: 0
        } catch (e: Exception) {
            log.warn("Failed to fetch oppgave count from mock: ${e.message}")
            0
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
            restTemplate.getForObject("$baseUrl$VERIFICATION_BASE_PATH/journalpost/count", Int::class.java) ?: 0
        } catch (e: Exception) {
            log.warn("Failed to fetch journalpost count from mock: ${e.message}")
            0
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
