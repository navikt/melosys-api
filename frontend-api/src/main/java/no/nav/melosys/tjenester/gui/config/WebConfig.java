package no.nav.melosys.tjenester.gui.config;

import java.time.LocalDate;
import java.time.LocalDateTime;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.module.kotlin.KotlinModule;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdInterceptor;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.config.jackson.MelosysModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.filter.UrlHandlerFilter;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private static final String API_PREFIX = "/api";
    private static final String FRONTEND_API_TJENESTER = "no.nav.melosys.tjenester.gui";
    private final ApiKeyInterceptor apiKeyInterceptor;

    public WebConfig(ApiKeyInterceptor apiKeyInterceptor) {
        this.apiKeyInterceptor = apiKeyInterceptor;
    }

    @Bean
    public JsonMapperBuilderCustomizer melosysJsonMapperCustomizer(@Lazy KodeverkService kodeverkService) {
        return builder -> builder
            .addModule(new KotlinModule.Builder().build())
            .addModule(new MelosysModule(kodeverkService))
            // Jackson tolker tall som epoch-day: {"periodeFom": 12345} ble stille til 2003-10-20.
            // Avvis heller tall som dato, slik at klienten får 400 i stedet for en feil dato.
            .withCoercionConfig(LocalDate.class, cfg -> cfg.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail))
            .withCoercionConfig(LocalDateTime.class, cfg -> cfg.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail))
            .enable(MapperFeature.DEFAULT_VIEW_INCLUSION);
    }

    @Bean
    public UrlHandlerFilter trailingSlashFilter() throws Exception {
        return UrlHandlerFilter.trailingSlashHandler("/**").wrapRequest().build();
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(API_PREFIX, WebConfig::erFrontendApiRestController);
    }

    private static boolean erFrontendApiRestController(Class<?> clazz) {
        return clazz.getPackageName().startsWith(FRONTEND_API_TJENESTER)
            && clazz.isAnnotationPresent(RestController.class);
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new CorrelationIdInterceptor());

        // test dette kun for ftrl admin så kan vi bytte fjerne AdminController for resten om det funker fint
        registry.addInterceptor(apiKeyInterceptor).addPathPatterns("/admin/**");
    }

}
