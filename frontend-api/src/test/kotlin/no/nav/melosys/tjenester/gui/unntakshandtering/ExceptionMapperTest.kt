package no.nav.melosys.tjenester.gui.unntakshandtering

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import jakarta.servlet.http.HttpServletRequest
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.exception.SikkerhetsbegrensningException
import no.nav.melosys.exception.TekniskException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.web.reactive.function.client.WebClientResponseException

@ExtendWith(MockKExtension::class)
class ExceptionMapperTest {

    private lateinit var exceptionMapper: ExceptionMapper

    @MockK
    private lateinit var request: HttpServletRequest

    @BeforeEach
    fun setup() {
        exceptionMapper = ExceptionMapper()
        every { request.remoteHost } returns "localhost"
        every { request.requestURI } returns "/test-uri"
    }

    @Test
    fun `skal håndtere funksjonell exception med status bad request`() {
        val melding = "Funksjonell feil"
        val funksjonellException = FunksjonellException(melding)
        assertResponse(exceptionMapper.håndter(funksjonellException, request), HttpStatus.BAD_REQUEST, melding)
    }

    @Test
    fun `skal håndtere teknisk exception med status internal server error`() {
        val melding = "Teknisk feil"
        val tekniskException = TekniskException(melding)
        assertResponse(exceptionMapper.håndter(tekniskException, request), HttpStatus.INTERNAL_SERVER_ERROR, melding)
    }

    @Test
    fun `skal håndtere JWT token unauthorized exception med status unauthorized`() {
        val jwtTokenUnauthorizedException = JwtTokenUnauthorizedException()
        assertResponse(exceptionMapper.håndter(jwtTokenUnauthorizedException, request), HttpStatus.UNAUTHORIZED, "JwtTokenUnauthorizedException")
    }

    @Test
    fun `skal håndtere sikkerhetsbegrensning exception med status forbidden`() {
        val melding = "Sikkerhetsfeil"
        val sikkerhetsbegrensningException = SikkerhetsbegrensningException(melding)
        assertResponse(exceptionMapper.håndter(sikkerhetsbegrensningException, request), HttpStatus.FORBIDDEN, melding)
    }

    @Test
    fun `skal håndtere ikke funnet exception med status not found`() {
        val melding = "Teknisk feil"
        val ikkeFunnetException = IkkeFunnetException(melding)
        assertResponse(exceptionMapper.håndter(ikkeFunnetException, request), HttpStatus.NOT_FOUND, melding)
    }

    @Test
    fun `skal håndtere ulesbar JSON med status bad request og warn-logg`() {
        val melding = "JSON parse error"
        val exception = HttpMessageNotReadableException(melding, MockHttpInputMessage(ByteArray(0)))

        val loggede = medLoggfanger {
            assertResponse(exceptionMapper.håndter(exception, request), HttpStatus.BAD_REQUEST, melding)
        }

        assertEquals(listOf(Level.WARN), loggede.map { it.level })
    }

    @Test
    fun `skal håndtere WebClientResponseException med JSON melding`() {
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
    fun `skal håndtere WebClientResponseException uten JSON melding`() {
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
    fun `skal håndtere WebClientResponseException med ugyldig JSON`() {
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

    // Fanger loggen fra ExceptionMapper.kt sin fil-logger, så vi kan slå fast at
    // klientfeil havner på WARN og ikke drukner ekte serverfeil på ERROR.
    private fun medLoggfanger(block: () -> Unit): List<ILoggingEvent> {
        val logger = LoggerFactory.getLogger(ExceptionMapper::class.java) as Logger
        val appender = ListAppender<ILoggingEvent>().apply { context = logger.loggerContext }
        appender.start()
        logger.addAppender(appender)
        try {
            block()
        } finally {
            logger.detachAppender(appender)
        }
        return appender.list
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
