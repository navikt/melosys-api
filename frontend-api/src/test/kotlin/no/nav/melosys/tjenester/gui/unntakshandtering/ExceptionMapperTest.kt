package no.nav.melosys.tjenester.gui.unntakshandtering;

import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.exception.SikkerhetsbegrensningException
import no.nav.melosys.exception.TekniskException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.reactive.function.client.WebClientResponseException
import jakarta.servlet.http.HttpServletRequest

@ExtendWith(MockitoExtension::class)
class ExceptionMapperTest {

    private lateinit var exceptionMapper: ExceptionMapper

    @Mock
    private lateinit var request: HttpServletRequest

    @BeforeEach
    fun setup() {
        exceptionMapper = ExceptionMapper()
    }

    @Test
    fun funksjonellException() {
        val melding = "Funksjonell feil"
        val funksjonellException = FunksjonellException(melding)
        assertResponse(exceptionMapper.håndter(funksjonellException, request), HttpStatus.BAD_REQUEST, melding)
    }

    @Test
    fun tekniskException() {
        val melding = "Teknisk feil"
        val tekniskException = TekniskException(melding)
        assertResponse(exceptionMapper.håndter(tekniskException, request), HttpStatus.INTERNAL_SERVER_ERROR, melding)
    }

    @Test
    fun jwtTokenUnauthorizedException_SkalLoggesSomWarn_ReturnerStatusForbidden() {
        val jwtTokenUnauthorizedException = JwtTokenUnauthorizedException()
        assertResponse(exceptionMapper.håndter(jwtTokenUnauthorizedException, request), HttpStatus.UNAUTHORIZED, "JwtTokenUnauthorizedException")
    }

    @Test
    fun sikkerhetsBegrensningException() {
        val melding = "Sikkerhetsfeil"
        val sikkerhetsbegrensningException = SikkerhetsbegrensningException(melding)
        assertResponse(exceptionMapper.håndter(sikkerhetsbegrensningException, request), HttpStatus.FORBIDDEN, melding)
    }

    @Test
    fun ikkeFunnetException() {
        val melding = "Teknisk feil"
        val ikkeFunnetException = IkkeFunnetException(melding)
        assertResponse(exceptionMapper.håndter(ikkeFunnetException, request), HttpStatus.NOT_FOUND, melding)
    }

    @Test
    fun webClientResponseException_medJSONMessage() {
        val responseBody = """{"message": "Client error occurred"}"""
        val webClientResponseException = WebClientResponseException(
            "Client error",
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.reasonPhrase,
            HttpHeaders.EMPTY,
            responseBody.toByteArray(),
            Charsets.UTF_8
        )

        val responseEntity = exceptionMapper.håndter(webClientResponseException, request)

        assertResponse(responseEntity, HttpStatus.INTERNAL_SERVER_ERROR, "Client error", listOf("Client error occurred"))
    }

    @Test
    fun webClientResponseException_utenJSONMessage() {
        val webClientResponseException = WebClientResponseException(
            "Client error without JSON",
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.reasonPhrase,
            HttpHeaders.EMPTY,
            ByteArray(0),
            Charsets.UTF_8
        )

        val responseEntity = exceptionMapper.håndter(webClientResponseException, request)

        assertResponse(responseEntity, HttpStatus.INTERNAL_SERVER_ERROR, "Client error without JSON")
    }

    @Test
    fun webClientResponseException_medUgyldigJSON() {
        val responseBody = """{"invalidJson": }"""
        val webClientResponseException = WebClientResponseException(
            "Client error",
            HttpStatus.BAD_REQUEST.value(),
            HttpStatus.BAD_REQUEST.reasonPhrase,
            HttpHeaders.EMPTY,
            responseBody.toByteArray(),
            Charsets.UTF_8
        )

        val responseEntity = exceptionMapper.håndter(webClientResponseException, request)

        assertResponse(responseEntity, HttpStatus.INTERNAL_SERVER_ERROR, "Client error")
    }

    private fun assertResponse(
        responseEntity: ResponseEntity<Map<String, Any>>,
        expectedStatus: HttpStatus,
        expectedMessage: String,
        forventetFeilmeldinger: List<String>? = null
    ) {
        assertEquals(expectedStatus, responseEntity.statusCode)
        assertTrue { responseEntity.body is Map<*, *> }
        assertEquals(expectedMessage, responseEntity.body!!["message"])
        if (forventetFeilmeldinger == null) {
            assertNull(responseEntity.body!!["feilkoder"])
        } else {
            assertEquals(forventetFeilmeldinger, responseEntity.body!!["feilkoder"])
        }
    }
}
