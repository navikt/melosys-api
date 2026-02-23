package no.nav.melosys.tjenester.gui.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ServletRequestPathUtils;

/**
 * Spring Boot 4.0 fjernet trailing slash matching. Denne filteren wrapper
 * innkommende requests med trailing slash slik at de matcher controller-mappings.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TrailingSlashFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path.length() > 1 && path.endsWith("/")) {
            String trimmedPath = path.substring(0, path.length() - 1);
            HttpServletRequest wrapped = new HttpServletRequestWrapper(request) {
                @Override
                public String getRequestURI() {
                    return trimmedPath;
                }

                @Override
                public StringBuffer getRequestURL() {
                    String url = super.getRequestURL().toString();
                    return new StringBuffer(url.substring(0, url.length() - 1));
                }

                @Override
                public String getServletPath() {
                    String sp = super.getServletPath();
                    return sp.endsWith("/") && sp.length() > 1 ? sp.substring(0, sp.length() - 1) : sp;
                }
            };
            ServletRequestPathUtils.parseAndCache(wrapped);
            filterChain.doFilter(wrapped, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
