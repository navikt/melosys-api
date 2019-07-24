package no.nav.melosys.integrasjon.felles.mdc;


import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.filter.OncePerRequestFilter;

@WebFilter(urlPatterns = "/*")
public class MDCFilter extends OncePerRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MDCFilter.class);
    private static final String SYSTEMBRUKER = "srvmelosys";

    @Override
    public void doFilterInternal(HttpServletRequest servletRequest, HttpServletResponse servletResponse, FilterChain filterChain)
        throws IOException, ServletException {
        try {
            final String userId = SubjectHandler.getInstance().getUserID();
            final String callId = MDCOperations.generateCallId();

            MDCOperations.putToMDC(MDCOperations.MDC_CALL_ID, callId);
            MDCOperations.putToMDC(MDCOperations.MDC_USER_ID, userId != null ? userId : "");
            MDCOperations.putToMDC(MDCOperations.MDC_CONSUMER_ID, SYSTEMBRUKER);
            LOGGER.debug("Set MDC values");

            filterChain.doFilter(servletRequest, servletResponse);

        } finally {
            MDCOperations.remove(MDCOperations.MDC_CALL_ID);
            MDCOperations.remove(MDCOperations.MDC_USER_ID);
            MDCOperations.remove(MDCOperations.MDC_CONSUMER_ID);
            LOGGER.debug("Cleared MDC values");
        }
    }
}
