package no.nav.melosys.tjenester.gui.config;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.SwaggerConfigLocator;
import io.swagger.jaxrs.config.SwaggerContextService;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@Profile("test")
public class SwaggerConfig implements
    ApplicationListener<ContextRefreshedEvent> {

    private final JerseyConfig jerseyConfig;

    @Autowired
    public SwaggerConfig(JerseyConfig jerseyConfig) {
        this.jerseyConfig = jerseyConfig;
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
            .select()
            .apis(RequestHandlerSelectors.basePackage("no.nav.melosys.tjenester.gui"))
            .paths(PathSelectors.any())
            .build()
            .enable(true);
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {

        // Registrere Swagger-Core
        jerseyConfig.register(ApiListingResource.class);
        jerseyConfig.register(SwaggerSerializers.class);

        // Konfigurasjon til Swagger
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setTitle("Melosys API");
        beanConfig.setVersion("0");
        beanConfig.setConfigId("melosys-api");
        beanConfig.setContact("Team Melosys");
        beanConfig.setSchemes(new String[]{"http", "https"});
        beanConfig.setBasePath("/api");
        beanConfig.setResourcePackage("no.nav.melosys.tjenester.gui");
        beanConfig.setPrettyPrint(true);
        beanConfig.setScan(true);
        SwaggerConfigLocator.getInstance().putConfig(SwaggerContextService.CONFIG_ID_DEFAULT, beanConfig);
    }
}
