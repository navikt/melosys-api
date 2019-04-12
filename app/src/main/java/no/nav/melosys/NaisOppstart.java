package no.nav.melosys;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("nais")
public class NaisOppstart implements Oppstart, EnvironmentAware {

    private static final String TRUST_STORE_KEY = "javax.net.ssl.trustStore";

    private static final Logger log = LoggerFactory.getLogger(NaisOppstart.class);

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
        // Navikt javabaseimaget setter keystore/passord automatisk
        log.debug("Truststore: {}", System.getProperty(TRUST_STORE_KEY));
        loadSystemProperties();
    }

    @Override
    public void loadProperty(String key) {
        Properties systemProperties = System.getProperties();
        if (systemProperties.get(key) == null) {
            String value = environment.getRequiredProperty(key);
            System.setProperty(key, environment.getRequiredProperty(key));
            log.debug("Property {} settes til {}", key, value);
        }
    }

}
