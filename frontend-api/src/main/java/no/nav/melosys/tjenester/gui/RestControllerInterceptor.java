package no.nav.melosys.tjenester.gui;

import no.nav.melosys.sikkerhet.context.ThreadLocalAccessInfo;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RestControllerInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String requestURI = request.getRequestURI();
        System.out.println("######### preHandle ################");
        System.out.println(requestURI);

        ThreadLocalAccessInfo.preHandle(requestURI);
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        String requestURI = request.getRequestURI();
        System.out.println("########### After ##############");
        System.out.println(requestURI);
        ThreadLocalAccessInfo.afterCompletion(requestURI);
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }

}

