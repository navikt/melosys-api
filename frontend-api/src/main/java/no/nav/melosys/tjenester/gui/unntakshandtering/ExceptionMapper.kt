package no.nav.melosys.tjenester.gui.unntakshandtering

import mu.KotlinLogging
import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.exception.SikkerhetsbegrensningException
import no.nav.melosys.exception.ValideringException
import no.nav.melosys.integrasjon.felles.mdc.MDCOperations
import no.nav.security.token.support.spring.validation.interceptor.JwtTokenUnauthorizedException
import org.slf4j.MDC
import org.slf4j.event.Level
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import javax.servlet.http.HttpServletRequest

private val log = KotlinLogging.logger { }

@ControllerAdvice
class ExceptionMapper {
    @ExceptionHandler(IkkeFunnetException::class)
    fun håndter(e: IkkeFunnetException, request: HttpServletRequest): ResponseEntity<Map<String, Any?>> =
        håndter(e, request, HttpStatus.NOT_FOUND, Level.WARN)

    @ExceptionHandler(FunksjonellException::class)
    fun håndter(e: FunksjonellException, request: HttpServletRequest): ResponseEntity<Map<String, Any?>> =
        håndter(e, request, HttpStatus.BAD_REQUEST, Level.WARN)

    @ExceptionHandler(SikkerhetsbegrensningException::class)
    fun håndter(e: SikkerhetsbegrensningException, request: HttpServletRequest): ResponseEntity<Map<String, Any?>> =
        håndter(e, request, HttpStatus.FORBIDDEN, Level.WARN)

    @ExceptionHandler(ValideringException::class)
    fun håndter(e: ValideringException, request: HttpServletRequest): ResponseEntity<Map<String, Any?>> =
        håndter(e, request, HttpStatus.BAD_REQUEST, Level.INFO, e.feilkoder)

    @ExceptionHandler(Exception::class)
    fun håndter(e: Exception, request: HttpServletRequest): ResponseEntity<Map<String, Any?>> =
        håndter(e, request, HttpStatus.INTERNAL_SERVER_ERROR, Level.ERROR)

    private fun håndter(
        e: Exception,
        request: HttpServletRequest,
        httpStatus: HttpStatus,
        loggnivå: Level,
        begrunnelser: Collection<*>? = emptyList<Any>()
    ): ResponseEntity<Map<String, Any?>> {
        val message = e.message ?: e.javaClass.simpleName
        val errorMessage =
            "API kall feilet: $message\nremoteHost:${request.remoteHost}\nrequestURI :${request.requestURI}"

        finnLoggnivå(e, loggnivå).let {
            if (it == Level.ERROR) {
                log.error(errorMessage, e)
            } else if (it == Level.WARN) {
                log.warn(errorMessage, e)
            }
        }

        val entity = mutableMapOf<String, Any?>(
            "status" to httpStatus.value(),
            "error" to httpStatus.reasonPhrase,
            "message" to message,
            "correlationId" to MDC.get(MDCOperations.CORRELATION_ID),
        ).apply {
            if (!begrunnelser.isNullOrEmpty()) {
                this["feilkoder"] = begrunnelser
            }
        }
        return ResponseEntity(entity, httpStatus)
    }

    private fun finnLoggnivå(e: Exception, loggnivå: Level) =
        if (loggnivå == Level.ERROR && e is JwtTokenUnauthorizedException) Level.WARN else loggnivå
}
