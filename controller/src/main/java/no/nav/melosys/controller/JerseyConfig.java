package no.nav.melosys;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

import no.nav.melosys.controller.BehandlingRestTjeneste;

@Configuration
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        register(BehandlingRestTjeneste.class);
    }
}