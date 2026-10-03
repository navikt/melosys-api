package no.nav.melosys.tjenester.gui;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.springframework.web.servlet.HandlerInterceptor;

public class RestControllerInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        ThreadLocalAccessInfo.beforeControllerRequest(request.getRequestURI(), isAdminRequest(request));
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        ThreadLocalAccessInfo.afterControllerRequest(request.getRequestURI());
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }

    private boolean isAdminRequest(HttpServletRequest request) {
        // Alle kall under /admin/ har passert AdminTilgangInterceptor og @Protected før controlleren kjører.
        // /api/admin/** går ikke gjennom AdminTilgangInterceptor, og skal derfor ikke regnes som admin-kall.
        return request.getRequestURI().startsWith("/admin/");
    }
}

