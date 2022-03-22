package no.nav.melosys.tjenester.gui.config;

import java.util.List;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.config.jackson.MelosysModule;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private static final String API_PREFIX = "/api";
    private static final String FRONTEND_API_TJENESTER = "no.nav.melosys.tjenester.gui";
    private final KodeverkService kodeverkService;

    public WebConfig(KodeverkService kodeverkService) {
        this.kodeverkService = kodeverkService;
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.removeIf(MappingJackson2HttpMessageConverter.class::isInstance);
        converters.add(new MappingJackson2HttpMessageConverter(apiObjectMapper()));
    }

    private ObjectMapper apiObjectMapper() {
        return Jackson2ObjectMapperBuilder.json()
            .modules(new JavaTimeModule(), new MelosysModule(kodeverkService))
            .featuresToEnable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(API_PREFIX, WebConfig::erFrontendApiRestTjeneste);
    }

    private static boolean erFrontendApiRestTjeneste(Class<?> clazz) {
        return clazz.getPackageName().startsWith(FRONTEND_API_TJENESTER)
            && clazz.isAnnotationPresent(RestController.class);
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        configurer.defaultContentType(MediaType.APPLICATION_JSON);
    }
}
