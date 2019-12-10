package no.nav.melosys.tjenester.gui.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private static final String API_PREFIX = "/api";
    private static final String FRONTEND_API_TJENESTER = "no.nav.melosys.tjenester.gui";

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(API_PREFIX, WebConfig::erFrontendApiTjeneste);
    }

    private static boolean erFrontendApiTjeneste(Class clazz) {
        return clazz.getPackageName().startsWith(FRONTEND_API_TJENESTER)
            && clazz.isAnnotationPresent(RestController.class);
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON);
    }
}
