package no.nav.melosys.tjenester.gui.unntakshandtering

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
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.mock.http.MockHttpInputMessage
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.reactive.function.client.WebClientResponseException

private const val UGYLDIG_FORESPØRSEL = "Ugyldig format på forespørselen"

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

    @Test
    fun `skal håndtere HttpMessageNotReadableException med sanert melding`() {
        val exception = HttpMessageNotReadableException(
            "JSON parse error: Cannot deserialize value of type `java.time.LocalDate` from String \"Invalid date\"",
            MockHttpInputMessage(ByteArray(0))
        )
        assertResponse(exceptionMapper.håndter(exception, request), HttpStatus.BAD_REQUEST, UGYLDIG_FORESPØRSEL)
    }

    @Test
    fun `skal håndtere MethodArgumentTypeMismatchException med sanert melding`() {
        val exception = MethodArgumentTypeMismatchException("abc", Long::class.java, "behandlingID", dummyMetodeParameter(), null)
        assertResponse(exceptionMapper.håndter(exception, request), HttpStatus.BAD_REQUEST, UGYLDIG_FORESPØRSEL)
    }

    @Test
    fun `skal håndtere MethodArgumentNotValidException med sanert melding og bevarte feilkoder`() {
        val bindingResult = BeanPropertyBindingResult(Any(), "unntaksperiodeRequestDto")
        bindingResult.addError(FieldError("unntaksperiodeRequestDto", "periodeFom", "must not be null"))
        val exception = MethodArgumentNotValidException(dummyMetodeParameter(), bindingResult)

        assertResponse(
            exceptionMapper.håndter(exception, request),
            HttpStatus.BAD_REQUEST,
            UGYLDIG_FORESPØRSEL,
            listOf("periodeFom: must not be null")
        )
    }

    @Test
    fun `skal ta med klasse-constraints fra globalErrors i feilkoder`() {
        val bindingResult = BeanPropertyBindingResult(Any(), "unntaksperiodeRequestDto")
        bindingResult.addError(ObjectError("unntaksperiodeRequestDto", "periodeFom må være før periodeTom"))
        val exception = MethodArgumentNotValidException(dummyMetodeParameter(), bindingResult)

        assertResponse(
            exceptionMapper.håndter(exception, request),
            HttpStatus.BAD_REQUEST,
            UGYLDIG_FORESPØRSEL,
            listOf("unntaksperiodeRequestDto: periodeFom må være før periodeTom")
        )
    }

    @Test
    fun `skal utlede status og klientvennlig melding fra ErrorResponse i catch-all`() {
        val exception = HttpMediaTypeNotSupportedException(
            MediaType.TEXT_PLAIN,
            listOf(MediaType.APPLICATION_JSON)
        )
        // ProblemDetail-teksten fra Spring er laget for klienten og sier noe konkret, i motsetning til
        // statusteksten "Unsupported Media Type"
        assertResponse(
            exceptionMapper.håndter(exception as Exception, request),
            HttpStatus.UNSUPPORTED_MEDIA_TYPE,
            exception.body.detail!!
        )
    }

    @Test
    fun `skal videreføre headere fra ErrorResponse slik at 405 får Allow`() {
        val exception = HttpRequestMethodNotSupportedException("DELETE", listOf("GET", "POST"))

        val responseEntity = exceptionMapper.håndter(exception as Exception, request)

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, responseEntity.statusCode)
        // RFC 9110 krever Allow på 405. Headerne ligger på ErrorResponse, ikke bare statuskoden.
        assertEquals("GET, POST", responseEntity.headers.getFirst(HttpHeaders.ALLOW))
    }

    @Test
    fun `catch-all gir fortsatt 500 for exceptions uten ErrorResponse`() {
        val melding = "Noe gikk galt"
        assertResponse(exceptionMapper.håndter(RuntimeException(melding) as Exception, request), HttpStatus.INTERNAL_SERVER_ERROR, melding)
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    private fun dummyMetodeForBinding(argument: String) = Unit

    private fun dummyMetodeParameter() =
        MethodParameter(ExceptionMapperTest::class.java.getDeclaredMethod("dummyMetodeForBinding", String::class.java), 0)

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
