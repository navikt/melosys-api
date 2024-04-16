package no.nav.melosys.integrasjon.felles.mdc

import no.nav.melosys.config.MDCOperations
import no.nav.melosys.config.MDCOperations.Companion.CORRELATION_ID
import no.nav.melosys.config.MDCOperations.Companion.getCorrelationId
import no.nav.melosys.sikkerhet.context.SubjectHandler
import org.slf4j.LoggerFactory
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.ModelAndView
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

class CorrelationIdInterceptor : HandlerInterceptor {

    private val LOGGER = LoggerFactory.getLogger(CorrelationIdInterceptor::class.java)
    private val SYSTEMBRUKER = "srvmelosys"

    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val userId = SubjectHandler.getInstance().userID
        val callId = MDCOperations.generateCallId()
        MDCOperations.putToMDC(MDCOperations.MDC_CALL_ID, callId)
        MDCOperations.putToMDC(
            MDCOperations.MDC_USER_ID,
            userId ?: ""
        )
        MDCOperations.putToMDC(CORRELATION_ID, getCorrelationId(request))
        MDCOperations.putToMDC(MDCOperations.MDC_CONSUMER_ID, SYSTEMBRUKER)
        LOGGER.debug("Set MDC values")
        return true
    }

    override fun postHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
        modelAndView: ModelAndView?
    ) {
        MDCOperations.remove(MDCOperations.MDC_CALL_ID)
        MDCOperations.remove(MDCOperations.MDC_USER_ID)
        MDCOperations.remove(MDCOperations.MDC_CONSUMER_ID)
        MDCOperations.remove(CORRELATION_ID)
        LOGGER.debug("Cleared MDC values")
    }

}
