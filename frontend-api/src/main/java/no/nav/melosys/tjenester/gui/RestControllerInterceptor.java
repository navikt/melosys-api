package no.nav.melosys.tjenester.gui;

import no.nav.melosys.service.AdminTjeneste;
import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RestControllerInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String adminAccessHeader = request.getHeader(AdminTjeneste.API_KEY_HEADER);
        ThreadLocalAccessInfo.beforeControllerRequest(request.getRequestURI(), adminAccessHeader != null);
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        ThreadLocalAccessInfo.afterControllerRequest(request.getRequestURI());
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }

}

