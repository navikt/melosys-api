package no.nav.melosys.tjenester.gui.unntakshandtering

import com.google.gson.JsonParser
import mu.KotlinLogging
import no.nav.melosys.config.MDCOperations
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.exception.SikkerhetsbegrensningException
import no.nav.melosys.exception.ValideringException
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.slf4j.MDC
import org.slf4j.event.Level
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.ErrorResponse
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.reactive.function.client.WebClientResponseException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.context.request.async.AsyncRequestNotUsableException
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.io.IOException

private val log = KotlinLogging.logger { }

private const val UGYLDIG_FORESPØRSEL = "Ugyldig format på forespørselen"

@ControllerAdvice
class ExceptionMapper {

    @ExceptionHandler(IkkeFunnetException::class)
    fun håndter(e: IkkeFunnetException, request: HttpServletRequest): ResponseEntity<Map<String, Any>> =
        håndter(e, request, HttpStatus.NOT_FOUND, Level.WARN)

    @ExceptionHandler(NoResourceFoundException::class)
    fun håndter(e: NoResourceFoundException, request: HttpServletRequest): ResponseEntity<Map<String, Any>> =
        håndter(e, request, HttpStatus.NOT_FOUND, Level.WARN)

    @ExceptionHandler(FunksjonellException::class)
    fun håndter(e: FunksjonellException, request: HttpServletRequest): ResponseEntity<Map<String, Any>> =
        håndter(e, request, HttpStatus.BAD_REQUEST, Level.WARN)

    @ExceptionHandler(SikkerhetsbegrensningException::class)
    fun håndter(e: SikkerhetsbegrensningException, request: HttpServletRequest): ResponseEntity<Map<String, Any>> =
        håndter(e, request, HttpStatus.FORBIDDEN, Level.WARN)

    @ExceptionHandler(JwtTokenUnauthorizedException::class)
    fun håndter(e: JwtTokenUnauthorizedException, request: HttpServletRequest): ResponseEntity<Map<String, Any>> =
        håndter(e, request, HttpStatus.UNAUTHORIZED, Level.WARN)

    @ExceptionHandler(ValideringException::class)
    fun håndter(e: ValideringException, request: HttpServletRequest): ResponseEntity<Map<String, Any>> =
        håndter(e, request, HttpStatus.BAD_REQUEST, Level.INFO, e.feilkoder)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun håndter(e: MethodArgumentNotValidException, request: HttpServletRequest): ResponseEntity<Map<String, Any>> {
        // Både felt-constraints (@NotNull på et felt) og klasse-constraints (globalErrors) må med,
        // ellers ville en klasse-constraint gitt 400 helt uten informasjon til klienten.
        val feilmeldinger = e.bindingResult.fieldErrors.map { "${it.field}: ${it.defaultMessage}" } +
            e.bindingResult.globalErrors.map { "${it.objectName}: ${it.defaultMessage}" }
        // Meldingen fra Spring inneholder full metodesignatur og klassesti - kun feilkodene er trygge å eksponere
        return håndter(e, request, HttpStatus.BAD_REQUEST, Level.INFO, feilmeldinger, UGYLDIG_FORESPØRSEL, loggStacktrace = false)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun håndter(e: HttpMessageNotReadableException, request: HttpServletRequest): ResponseEntity<Map<String, Any>> =
        håndter(e, request, HttpStatus.BAD_REQUEST, Level.INFO, responsMelding = UGYLDIG_FORESPØRSEL, loggStacktrace = false)

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun håndter(e: MethodArgumentTypeMismatchException, request: HttpServletRequest): ResponseEntity<Map<String, Any>> =
        håndter(e, request, HttpStatus.BAD_REQUEST, Level.INFO, responsMelding = UGYLDIG_FORESPØRSEL, loggStacktrace = false)

    @ExceptionHandler(WebClientResponseException::class)
    fun håndter(e: WebClientResponseException, request: HttpServletRequest): ResponseEntity<Map<String, Any>> {
        val feilmeldingFraRespons = hentMessageFraJsonStreng(e.responseBodyAsString)
        return håndter(e, request, HttpStatus.INTERNAL_SERVER_ERROR, Level.ERROR, listOfNotNull(feilmeldingFraRespons))
    }

    @ExceptionHandler(IOException::class)
    fun håndter(e: IOException, request: HttpServletRequest): ResponseEntity<Map<String, Any>> {
        return if (e.message?.contains("Broken pipe") == true) {
            håndter(e, request, HttpStatus.SERVICE_UNAVAILABLE, Level.WARN)
        } else {
            håndter(e, request, HttpStatus.INTERNAL_SERVER_ERROR, Level.ERROR)
        }
    }

    @ExceptionHandler(AsyncRequestNotUsableException::class)
    fun håndter(e: AsyncRequestNotUsableException, request: HttpServletRequest) {
        // Klienten avbrøt requesten før respons var ferdig - ikke logg stacktrace
        log.debug { "Klient avbrøt request: ${request.requestURI}" }
    }

    @ExceptionHandler(Exception::class)
    fun håndter(e: Exception, request: HttpServletRequest): ResponseEntity<Map<String, Any>> {
        // Spring-exceptions som implementerer ErrorResponse bærer sin egen HTTP-status (415, 405, 400 osv.).
        // Uten dette ville denne catch-allen gjort alle rene klientfeil om til 500.
        val errorResponse = e as? ErrorResponse
        val status: HttpStatusCode = errorResponse?.statusCode ?: HttpStatus.INTERNAL_SERVER_ERROR
        val erKlientfeil = status.is4xxClientError
        return håndter(
            e,
            request,
            status,
            if (erKlientfeil) Level.WARN else Level.ERROR,
            // e.message kan inneholde klassesti og metodesignatur, men ProblemDetail-teksten fra Spring er laget
            // for å vises til klienten ("Required parameter 'x' is not present."). Den er både trygg og mer
            // presis enn en fast melding, så vi bruker den når den finnes.
            responsMelding = if (erKlientfeil) errorResponse?.body?.detail ?: statustekst(status) else null,
            loggStacktrace = !erKlientfeil,
            // 405 og 415 er ikke gyldige uten Allow/Accept (RFC 9110). Headerne ligger på ErrorResponse,
            // og forsvant tidligere fordi vi bare plukket status derfra.
            headere = errorResponse?.headers ?: HttpHeaders.EMPTY
        )
    }

    private fun håndter(
        e: Exception,
        request: HttpServletRequest,
        httpStatus: HttpStatusCode,
        loggnivå: Level,
        begrunnelser: Collection<*>? = emptyList<Any>(),
        responsMelding: String? = null,
        loggStacktrace: Boolean = true,
        headere: HttpHeaders = HttpHeaders.EMPTY
    ): ResponseEntity<Map<String, Any>> {
        val message = e.message ?: e.javaClass.simpleName
        val errorMessage = buildString {
            appendLine("API kall feilet: $message")
            appendLine("remoteHost: ${request.remoteHost}")
            append("requestURI: ${request.requestURI}")
        }

        // Klientfeil (ugyldig JSON, feil datoformat) skjer per tastetrykk fra frontend. Stacktrace der er ren
        // logstøy uten diagnostisk verdi - selve meldingen sier alt vi trenger.
        if (loggStacktrace) {
            when (loggnivå) {
                Level.ERROR -> log.error(errorMessage, e)
                Level.WARN -> log.warn(errorMessage, e)
                else -> log.info(errorMessage, e)
            }
        } else {
            val utenStacktrace = "$errorMessage${System.lineSeparator()}exception: ${e.javaClass.simpleName}"
            when (loggnivå) {
                Level.ERROR -> log.error { utenStacktrace }
                Level.WARN -> log.warn { utenStacktrace }
                else -> log.info { utenStacktrace }
            }
        }

        val body = mapOf(
            "status" to httpStatus.value(),
            "error" to statustekst(httpStatus),
            "message" to (responsMelding ?: message),
            "correlationId" to MDC.get(MDCOperations.CORRELATION_ID)
        ) + if (!begrunnelser.isNullOrEmpty()) mapOf("feilkoder" to begrunnelser) else emptyMap<String, Any>()

        return ResponseEntity(body, headere, httpStatus)
    }

    // HttpStatus dekker kun standardkodene. Et ukjent statusnummer skal fortsatt beholde sin egen status
    // i stedet for å degraderes til 500, så teksten faller tilbake til statusnummeret.
    private fun statustekst(status: HttpStatusCode): String =
        HttpStatus.resolve(status.value())?.reasonPhrase ?: status.value().toString()

    private fun hentMessageFraJsonStreng(jsonString: String): String? =
        runCatching {
            JsonParser.parseString(jsonString)
                .asJsonObject
                .takeIf { it.has("message") }
                ?.get("message")
                ?.asString
        }.getOrNull()
}
