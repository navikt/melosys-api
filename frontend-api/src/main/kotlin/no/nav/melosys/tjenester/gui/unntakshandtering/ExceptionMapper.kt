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
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.reactive.function.client.WebClientResponseException
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.servlet.resource.NoResourceFoundException
import java.io.IOException

private val log = KotlinLogging.logger { }

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

    @ExceptionHandler(Exception::class)
    fun håndter(e: Exception, request: HttpServletRequest): ResponseEntity<Map<String, Any>> =
        håndter(e, request, HttpStatus.INTERNAL_SERVER_ERROR, Level.ERROR)

    private fun håndter(
        e: Exception,
        request: HttpServletRequest,
        httpStatus: HttpStatus,
        loggnivå: Level,
        begrunnelser: Collection<*>? = emptyList<Any>()
    ): ResponseEntity<Map<String, Any>> {
        val message = e.message ?: e.javaClass.simpleName
        val errorMessage = buildString {
            appendLine("API kall feilet: $message")
            appendLine("remoteHost: ${request.remoteHost}")
            append("requestURI: ${request.requestURI}")
        }

        when (loggnivå) {
            Level.ERROR -> log.error(errorMessage, e)
            Level.WARN -> log.warn(errorMessage, e)
            else -> log.info(errorMessage, e)
        }

        val body = mapOf(
            "status" to httpStatus.value(),
            "error" to httpStatus.reasonPhrase,
            "message" to message,
            "correlationId" to MDC.get(MDCOperations.CORRELATION_ID)
        ) + if (!begrunnelser.isNullOrEmpty()) mapOf("feilkoder" to begrunnelser) else emptyMap<String, Any>()

        return ResponseEntity(body, httpStatus)
    }

    private fun hentMessageFraJsonStreng(jsonString: String): String? =
        runCatching {
            JsonParser.parseString(jsonString)
                .asJsonObject
                .takeIf { it.has("message") }
                ?.get("message")
                ?.asString
        }.getOrNull()
}
