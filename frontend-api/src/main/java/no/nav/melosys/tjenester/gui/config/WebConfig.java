package no.nav.melosys.tjenester.gui.config;

import java.time.LocalDate;
import java.time.LocalDateTime;

import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.module.kotlin.KotlinModule;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import no.nav.melosys.integrasjon.felles.mdc.CorrelationIdInterceptor;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.tjenester.gui.config.jackson.MelosysModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
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
    private final ObjectFactory<JsonMapper> jsonMapper;

    public WebConfig(ApiKeyInterceptor apiKeyInterceptor, @Lazy ObjectFactory<JsonMapper> jsonMapper) {
        this.apiKeyInterceptor = apiKeyInterceptor;
        this.jsonMapper = jsonMapper;
    }

    @Bean
    public JsonMapperBuilderCustomizer melosysJsonMapperCustomizer(@Lazy KodeverkService kodeverkService) {
        return builder -> builder
            .addModule(new KotlinModule.Builder().build())
            .addModule(new MelosysModule(kodeverkService))
            .enable(MapperFeature.DEFAULT_VIEW_INCLUSION);
    }

    /**
     * Jackson tolker tall som epoch-day: {@code {"periodeFom": 12345}} ble stille til 2003-10-20.
     * Vi avviser heller tall som dato, slik at klienten får 400 i stedet for en feil dato.
     * <p>
     * Gjelder bevisst kun LocalDate/LocalDateTime. For Instant og OffsetDateTime er et tall epoch-tid, altså
     * en veldefinert og vanlig representasjon - der ville en avvisning brutt gyldige requester i stedet for
     * å fange en feil. (De går uansett utenom coercion-mekanismen, verifisert.)
     * <p>
     * Konfigurasjonen ligger bevisst på HTTP-converteren og ikke på {@code JsonMapperBuilderCustomizer}:
     * den customizeren bygger Boots delte ObjectMapper, som også brukes av Kafka-consumerne (se KafkaConfig).
     * Der ville en deserialiseringsfeil stoppet containeren via SkippableKafkaErrorHandler og krevd manuell
     * opprydding. Problemet vi løser er rent frontend-vendt, så rekkevidden holdes til MVC.
     * <p>
     * Mapperen bygges videre fra den delte, slik at moduler og features ikke kan drifte fra hverandre.
     */
    @Override
    public void configureMessageConverters(HttpMessageConverters.ServerBuilder builder) {
        JsonMapper mvcMapper = jsonMapper.getObject().rebuild()
            .withCoercionConfig(LocalDate.class, cfg -> cfg.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail))
            .withCoercionConfig(LocalDateTime.class, cfg -> cfg.setCoercion(CoercionInputShape.Integer, CoercionAction.Fail))
            .build();
        builder.withJsonConverter(new JacksonJsonHttpMessageConverter(mvcMapper));
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
