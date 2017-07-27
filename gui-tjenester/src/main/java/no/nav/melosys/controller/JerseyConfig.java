package no.nav.melosys.controller;

import org.glassfish.jersey.server.ResourceConfig;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        register(BehandlingRestTjeneste.class);
        register(FagsakRestTjeneste.class);
        register(PersonRestTjeneste.class);
    }
}