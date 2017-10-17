package no.nav.melosys.integrasjon.felles.mdc;


import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebFilter(urlPatterns = "/*")
public class MDCFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(MDCFilter.class);

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        try {
            String userId = ""; // FIXME Francois
            String consumerId = "srvmelosys";
            String callId = MDCOperations.generateCallId();

            MDCOperations.putToMDC(MDCOperations.MDC_CALL_ID, callId);
            MDCOperations.putToMDC(MDCOperations.MDC_USER_ID, userId);
            MDCOperations.putToMDC(MDCOperations.MDC_CONSUMER_ID, consumerId);
            LOGGER.debug("Set MDC values");

            filterChain.doFilter(servletRequest, servletResponse);

        } finally {
            MDCOperations.remove(MDCOperations.MDC_CALL_ID);
            MDCOperations.remove(MDCOperations.MDC_USER_ID);
            MDCOperations.remove(MDCOperations.MDC_CONSUMER_ID);
            LOGGER.debug("Cleared MDC values");
        }
    }

    @Override
    public void destroy() {

    }

}
