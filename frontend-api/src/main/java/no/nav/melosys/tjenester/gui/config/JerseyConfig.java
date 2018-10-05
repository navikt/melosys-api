package no.nav.melosys.tjenester.gui.config;

import javax.ws.rs.ApplicationPath;

import no.nav.melosys.tjenester.gui.*;
import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
@ApplicationPath("api")
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        setApplicationName("melosys-api");
        property(CommonProperties.FEATURE_AUTO_DISCOVERY_DISABLE, true);
        property(CommonProperties.METAINF_SERVICES_LOOKUP_DISABLE, true);

        // N.B. alfabetisk rekkefølge
        register(ArbeidsforholdHistorikkTjeneste.class);
        register(DokMotQueueTestRestTjeneste.class);
        register(DokumentTjeneste.class);
        register(FagsakTjeneste.class);
        register(AvklarteFaktaTjeneste.class);
        register(FrontendLoggingTjeneste.class);
        register(InngangTjeneste.class);
        register(JournalfoeringTjeneste.class);
        register(KodeverkTjeneste.class);
        register(OppgaveTjeneste.class);
        register(OrganisasjonTjeneste.class);
        register(PersonTjeneste.class);
        register(SaksbehandlerTjeneste.class);
        register(SaksflytTjeneste.class);
        register(SaksopplysningTjeneste.class);
        register(SoeknadTjeneste.class);
        register(VilkaarTjeneste.class);
    }
}