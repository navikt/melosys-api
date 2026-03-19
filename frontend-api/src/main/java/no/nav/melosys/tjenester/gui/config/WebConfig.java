package no.nav.melosys.tjenester.gui.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdInterceptor;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.config.jackson.MelosysModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.UrlHandlerFilter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private static final String API_PREFIX = "/api";
    private static final String FRONTEND_API_TJENESTER = "no.nav.melosys.tjenester.gui";
    private final ApiKeyInterceptor apiKeyInterceptor;
    private final KodeverkService kodeverkService;

    public WebConfig(ApiKeyInterceptor apiKeyInterceptor, @Lazy KodeverkService kodeverkService) {
        this.apiKeyInterceptor = apiKeyInterceptor;
        this.kodeverkService = kodeverkService;
    }

    /**
     * Legger Jackson 2-konverter med MelosysModule FØRST i listen slik at den
     * tar prioritet over Spring MVC 7 sin standard Jackson 3-konverter.
     * Jackson 3 brukes ikke i denne modulen.
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(0, createMvcJackson2Converter());
    }

    MappingJackson2HttpMessageConverter createMvcJackson2Converter() {
        var mapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .addModule(new KotlinModule.Builder().build())
            .addModule(new MelosysModule(kodeverkService))
            .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();
        return new MappingJackson2HttpMessageConverter(mapper);
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
        registry.addInterceptor(apiKeyInterceptor).addPathPatterns("/admin/**");
    }

}
