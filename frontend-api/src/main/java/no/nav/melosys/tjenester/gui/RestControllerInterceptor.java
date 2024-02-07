package no.nav.melosys.tjenester.gui;

import no.nav.melosys.service.AdminTjeneste;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
        boolean hasApiKeyHeader = request.getHeader(AdminTjeneste.API_KEY_HEADER) != null;
        boolean requestStartsWithAdmin = request.getRequestURI().startsWith("/admin/");
        return hasApiKeyHeader && requestStartsWithAdmin;
    }
}

