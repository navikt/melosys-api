package no.nav.melosys.itest

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
     * Henter alle MEDL-medlemskapsunntak lagret i mocken.
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
     * Henter antall MEDL-medlemskapsunntak.
     */
    fun medlCount(): Int {
        return try {
            // Docker-mock returnerer CountResponse(count), in-process mock kan returnere Int direkte
            val response = restTemplate.getForObject("$baseUrl$VERIFICATION_BASE_PATH/medl/count", CountResponse::class.java)
            response?.count ?: 0
        } catch (e: Exception) {
            log.warn("Failed to fetch MEDL count from mock: ${e.message}")
            0
        }
    }

    // ==================== SAK ====================

    /**
     * Henter alle saker lagret i mocken.
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
     * Henter antall saker.
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
     * Henter sak etter fagsakNr.
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
     * Henter SED-typer sendt for en spesifikk RINA-sak.
     * Returnerer liste med SED-typestrenger (f.eks. ["A012", "X008", "A004"]).
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
     * Henter alle SED-repo-data (rinaSaksnummer -> liste med SED-typer).
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
     * Henter all BUC-informasjon lagret i mocken.
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
     * Henter alle saksrelasjoner lagret i mocken.
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
     * Henter alle oppgaver lagret i mocken.
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
     * Henter antall oppgaver.
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
     * Henter oppgaver etter type (f.eks. "JFR", "BEH_SED").
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
     * Henter alle journalposter lagret i mocken.
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
     * Henter antall journalposter.
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
     * Henter journalpost etter ID.
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
     * Henter journalposter etter saksnummer.
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

    // ==================== OPPSUMMERING ====================

    /**
     * Henter oppsummering av alle mock-data-antall.
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

    // ==================== TØMMING ====================

    /**
     * Tømmer alle mock-data.
     * Bør kalles i @AfterEach for å sikre testisolasjon.
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

    // ==================== HELSE ====================

    /**
     * Sjekker om mocken er sunn og tilgjengelig.
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
