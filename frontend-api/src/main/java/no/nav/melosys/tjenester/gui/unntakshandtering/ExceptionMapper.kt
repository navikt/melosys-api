package no.nav.melosys.tjenester.gui.unntakshandtering

import no.nav.melosys.exception.FunksjonellException
import no.nav.melosys.exception.IkkeFunnetException
import no.nav.melosys.exception.SikkerhetsbegrensningException
import no.nav.melosys.exception.ValideringException
import no.nav.melosys.integrasjon.felles.mdc.MDCOperations
import org.slf4j.LoggerFactory
import org.slf4j.MDC
import org.slf4j.event.Level
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import javax.servlet.http.HttpServletRequest

@ControllerAdvice
class ExceptionMapper {
    @ExceptionHandler(value = [IkkeFunnetException::class])
    fun håndter(e: IkkeFunnetException, request: HttpServletRequest): ResponseEntity<Map<String, Any?>> {
        return håndter(e, request, HttpStatus.NOT_FOUND, Level.WARN)
    }

    @ExceptionHandler(value = [FunksjonellException::class])
    fun håndter(e: FunksjonellException, request: HttpServletRequest): ResponseEntity<Map<String, Any?>> {
        return håndter(e, request, HttpStatus.BAD_REQUEST, Level.WARN)
    }

    @ExceptionHandler(value = [SikkerhetsbegrensningException::class])
    fun håndter(e: SikkerhetsbegrensningException, request: HttpServletRequest): ResponseEntity<Map<String, Any?>> {
        return håndter(e, request, HttpStatus.FORBIDDEN, Level.WARN)
    }

    @ExceptionHandler(value = [ValideringException::class])
    fun håndter(e: ValideringException, request: HttpServletRequest): ResponseEntity<Map<String, Any?>> {
        return håndter(e, request, HttpStatus.BAD_REQUEST, Level.INFO, e.feilkoder)
    }

    @ExceptionHandler(value = [Exception::class])
    fun håndter(e: Exception, request: HttpServletRequest): ResponseEntity<Map<String, Any?>> {
        return håndter(e, request, HttpStatus.INTERNAL_SERVER_ERROR, Level.ERROR)
    }

    private fun håndter(
        e: Exception,
        request: HttpServletRequest,
        httpStatus: HttpStatus,
        loggnivå: Level,
        begrunnelser: Collection<*>? = emptyList<Any>()
    ): ResponseEntity<Map<String, Any?>> {
        val message = if (e.message != null) e.message else e.javaClass.simpleName
        if (loggnivå == Level.ERROR) {
            log.error(
                "API kall feilet: {}\nremoteHost:{}\nrequestURI :{}",
                message,
                request.remoteHost,
                request.requestURI,
                e
            )
        } else if (loggnivå == Level.WARN) {
            log.warn(
                "API kall feilet: {}\nremoteHost:{}\nrequestURI :{}",
                message,
                request.remoteHost,
                request.requestURI,
                e
            )
        }
        val entity: MutableMap<String, Any?> = HashMap()
        entity["status"] = httpStatus.value()
        entity["error"] = httpStatus.reasonPhrase
        entity["message"] = message
        entity["correlationId"] = MDC.get(MDCOperations.CORRELATION_ID)
        if (begrunnelser != null && !begrunnelser.isEmpty()) {
            entity["feilkoder"] = begrunnelser
        }
        return ResponseEntity(entity, httpStatus)
    }

    companion object {
        private val log = LoggerFactory.getLogger(ExceptionMapper::class.java)
    }
}
