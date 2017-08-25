package no.nav.melosys.tjenester.gui.config;

import javax.ws.rs.ApplicationPath;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import no.nav.melosys.tjenester.gui.BehandlingRestTjeneste;
import no.nav.melosys.tjenester.gui.FagsakRestTjeneste;
import no.nav.melosys.tjenester.gui.PersonRestTjeneste;
import no.nav.melosys.tjenester.gui.SaksbehandlerTjeneste;

@Configuration
@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        setApplicationName("melosys");
        register(BehandlingRestTjeneste.class);
        register(FagsakRestTjeneste.class);
        register(PersonRestTjeneste.class);
        register(SaksbehandlerTjeneste.class);
        configureSwagger();
    }

    private void configureSwagger() {
        // Registrere Swagger-Core
        register(ApiListingResource.class);
        register(SwaggerSerializers.class);

        // Konfigurasjon til Swagger
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setTitle("Melosys GUI tjenester");
        beanConfig.setVersion("0");
        beanConfig.setConfigId("gui-tjenester");
        beanConfig.setContact("Team Melosys");
        beanConfig.setSchemes(new String[] {"http", "https"});
        beanConfig.setBasePath("/");
        beanConfig.setResourcePackage("no.nav.melosys.regler");
        beanConfig.setPrettyPrint(true);
        beanConfig.setScan(true);
    }
}