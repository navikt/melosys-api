package no.nav.melosys.saksflyt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.PropertySource;

/**
 * Spring konfigurasjon
 */
@SpringBootApplication
@PropertySource("classpath:saksflyt.properties")
public class SaksflytApplication {

    @SuppressWarnings("resource")
    public static void main(String[] args) {
        ConfigurableApplicationContext ctx = SpringApplication.run(SaksflytApplication.class, args);
        ctx.registerShutdownHook(); // FIXME (farjam): Tror ikke vi trenger dette. Sjekk om PreDestroy metoder kalles hvis dette fjernes.
    }

}
