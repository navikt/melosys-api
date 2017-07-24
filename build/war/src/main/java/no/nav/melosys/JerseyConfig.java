package no.nav.melosys;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

import no.nav.melosys.controller.BehandlingRestTjeneste;
import no.nav.melosys.controller.JaxRsEndpoint;

@Configuration
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        register(JaxRsEndpoint.class);
        register(BehandlingRestTjeneste.class);
    }
}