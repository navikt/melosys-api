package no.nav.melosys.tjenester.gui.config;

import java.util.Arrays;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.config.SwaggerConfigLocator;
import io.swagger.jaxrs.config.SwaggerContextService;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.core.env.Environment;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@Profile("test")
public class SwaggerConfig implements
    ApplicationListener<ContextRefreshedEvent> {

    private final Environment environment;

    private final JerseyConfig jerseyConfig;

    @Autowired
    public SwaggerConfig(Environment environment, JerseyConfig jerseyConfig) {
        this.environment = environment;
        this.jerseyConfig = jerseyConfig;
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

        if (Arrays.asList(environment.getActiveProfiles()).contains("local")) {
            beanConfig.setBasePath("melosys/api");
        } else {
            beanConfig.setBasePath("/api");
        }
        beanConfig.setResourcePackage("no.nav.melosys.tjenester.gui");
        beanConfig.setPrettyPrint(true);
        beanConfig.setScan(true);
        SwaggerConfigLocator.getInstance().putConfig(SwaggerContextService.CONFIG_ID_DEFAULT, beanConfig);
    }
}
