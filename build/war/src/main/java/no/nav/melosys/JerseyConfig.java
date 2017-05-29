package no.nav.melosys;

        import no.nav.melosys.controller.JaxRsEndpoint;
        import org.glassfish.jersey.server.ResourceConfig;
        import org.springframework.context.annotation.Configuration;

@Configuration
public class JerseyConfig extends ResourceConfig {

    public JerseyConfig() {
        register(JaxRsEndpoint.class);
    }
}