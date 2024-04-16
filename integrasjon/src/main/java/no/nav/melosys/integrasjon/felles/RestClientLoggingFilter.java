package no.nav.melosys.integrasjon.felles;

import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestClientLoggingFilter implements ClientRequestFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(RestClientLoggingFilter.class);

    @Override
    public void filter(ClientRequestContext requestContext) {
        StringBuilder sb = new StringBuilder();
        sb.append(" - Path: ").append(requestContext.getUri().toString());
        if (LOGGER.isTraceEnabled()) {
            sb.append(" - Headers: ").append(requestContext.getHeaders());
        }
        sb.append(" - Entity: ").append(requestContext.getEntity());
        LOGGER.debug("HTTP REQUEST : {}", sb);
    }

}
