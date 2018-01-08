package no.nav.melosys.tjenester.gui.config;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import no.nav.melosys.tjenester.gui.*;
import no.nav.melosys.tjenester.gui.patch.JsonPatchReader;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

import javax.ws.rs.ApplicationPath;

@Configuration
@ApplicationPath("/api")
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        setApplicationName("melosys");
        register(ArbeidsforholdHistorikkTjeneste.class);
        register(BehandlingTjeneste.class);
        register(FagsakTjeneste.class);
        register(JsonPatchReader.class);
        register(RegelmodulTjeneste.class);
        register(SaksbehandlerTjeneste.class);
        register(SoknadTjeneste.class);
        register(SokTjeneste.class);
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