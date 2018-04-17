package no.nav.melosys.tjenester.gui.config;

import javax.ws.rs.ApplicationPath;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import no.nav.melosys.tjenester.gui.ArbeidsforholdHistorikkTjeneste;
import no.nav.melosys.tjenester.gui.BehandlingTjeneste;
import no.nav.melosys.tjenester.gui.FagsakTjeneste;
import no.nav.melosys.tjenester.gui.FaktaavklaringTjeneste;
import no.nav.melosys.tjenester.gui.JournalforingTjeneste;
import no.nav.melosys.tjenester.gui.KodeverkTjeneste;
import no.nav.melosys.tjenester.gui.OppgaveTjeneste;
import no.nav.melosys.tjenester.gui.SaksbehandlerTjeneste;
import no.nav.melosys.tjenester.gui.SokTjeneste;
import no.nav.melosys.tjenester.gui.SoknadTjeneste;
import no.nav.melosys.tjenester.gui.VurderingTjeneste;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
@ApplicationPath("api")
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        setApplicationName("melosys-api");
        // N.B. alfabetisk rekkefølge
        register(ArbeidsforholdHistorikkTjeneste.class);
        register(BehandlingTjeneste.class);
        register(FagsakTjeneste.class);
        register(FaktaavklaringTjeneste.class);
        register(JournalforingTjeneste.class);
        register(KodeverkTjeneste.class);
        register(OppgaveTjeneste.class);
        register(SaksbehandlerTjeneste.class);
        register(SoknadTjeneste.class);
        register(SokTjeneste.class);
        register(VurderingTjeneste.class);
        configureSwagger();
    }

    private void configureSwagger() {
        // Registrere Swagger-Core
        register(ApiListingResource.class);
        register(SwaggerSerializers.class);

        // Konfigurasjon til Swagger
        BeanConfig beanConfig = new BeanConfig();
        beanConfig.setTitle("Melosys API");
        beanConfig.setVersion("0");
        beanConfig.setConfigId("melosys-api");
        beanConfig.setContact("Team Melosys");
        beanConfig.setSchemes(new String[] {"http", "https"});
        beanConfig.setBasePath("/");
        beanConfig.setResourcePackage("no.nav.melosys.tjenester.gui");
        beanConfig.setPrettyPrint(true);
        beanConfig.setScan(true);
    }
}