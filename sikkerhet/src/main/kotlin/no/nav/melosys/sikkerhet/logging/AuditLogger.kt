package no.nav.melosys.sikkerhet.logging

import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import javax.servlet.http.HttpServletRequest

private val audit = KotlinLogging.logger("auditLogger")
private val logger = KotlinLogging.logger {}

@Component
class AuditLogger {
    fun log(auditEvent: AuditEvent) {
        audit.info { createCommonEventFormatString(auditEvent) }
    }

    private fun createCommonEventFormatString(auditEvent: AuditEvent): String {
        val request = httpServletRequest()
        if (request == null) {
            logger.warn { "Audit er ment å brukes i kontekst av en HTTP request fra en saksbehandler." }
        }
        return createHeader(auditEvent.type) + createExtension(
            auditEvent,
            request
        ).toString()
    }

    private fun httpServletRequest(): HttpServletRequest? {
        return RequestContextHolder.getRequestAttributes()
            ?.takeIf { it is ServletRequestAttributes }
            ?.let { it as ServletRequestAttributes }
            ?.request
    }

    private fun createHeader(eventType: AuditEventType) =
        "CEF:0|melosys|Auditlog|1.0|audit:${eventType.name.lowercase()}|Medlemskap og lovvalg|INFO|"

    private fun createExtension(auditEvent: AuditEvent, request: HttpServletRequest?): LogExtension {
        return LogExtension(
            suid = auditEvent.sourceUserId,
            duid = auditEvent.destinationUserId,
            requestMethod = request?.method,
            requestURI = request?.requestURI,
            message = auditEvent.message,
            sourceProcessName = auditEvent.sourceProcessName
        )
    }

    data class LogExtension(
        val suid: String,
        val duid: String,
        val requestMethod: String?,
        val requestURI: String?,
        val message: String?,
        val sourceProcessName: String?,
        val end: Long = System.currentTimeMillis()
    ) {
        override fun toString(): String {
            return "suid=$suid " +
                "duid=$duid " +
                "end=$end " +
                "requestMethod=${requestMethod ?: "N/A"} " +
                "request=${requestURI ?: "N/A"} " +
                "sproc=${sourceProcessName ?: "N/A"} " +
                if (message != null) "msg=$message " else ""
        }
    }
}
