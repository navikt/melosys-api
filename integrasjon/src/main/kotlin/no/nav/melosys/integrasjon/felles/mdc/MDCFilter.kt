package no.nav.melosys.integrasjon.felles.mdc

import no.nav.melosys.sikkerhet.context.SubjectHandler
import org.slf4j.LoggerFactory
import org.springframework.web.filter.OncePerRequestFilter
import javax.servlet.FilterChain
import javax.servlet.annotation.WebFilter
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

@WebFilter(urlPatterns = ["/*"])
class MDCFilter : OncePerRequestFilter() {

    private val LOGGER = LoggerFactory.getLogger(MDCFilter::class.java)
    private val SYSTEMBRUKER = "srvmelosys"

    override fun doFilterInternal(
        servletRequest: HttpServletRequest,
        servletResponse: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            val userId = SubjectHandler.getInstance().userID
            val callId = MDCOperations.generateCallId()
            MDCOperations.putToMDC(MDCOperations.MDC_CALL_ID, callId)
            MDCOperations.putToMDC(
                MDCOperations.MDC_USER_ID,
                userId ?: ""
            )
            MDCOperations.putToMDC(MDCOperations.MDC_CONSUMER_ID, SYSTEMBRUKER)
            LOGGER.debug("Set MDC values")
            filterChain.doFilter(servletRequest, servletResponse)
        } finally {
            MDCOperations.remove(MDCOperations.MDC_CALL_ID)
            MDCOperations.remove(MDCOperations.MDC_USER_ID)
            MDCOperations.remove(MDCOperations.MDC_CONSUMER_ID)
            LOGGER.debug("Cleared MDC values")
        }
    }
}
