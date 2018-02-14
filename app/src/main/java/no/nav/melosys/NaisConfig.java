package no.nav.melosys;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

@Configuration
@Profile("nais")
public class NaisConfig implements Config, EnvironmentAware {

    private static final String TRUST_STORE_KEY = "javax.net.ssl.trustStore";

    private static final Logger log = LoggerFactory.getLogger(NaisConfig.class);

    private static Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;

        configureSsl();
        loadSystemProperties();
    }

    @Override
    public void configureSsl() {
        // Navikt javabaseimaget setter keystore/passord automatisk
        log.debug("Truststore: " + System.getProperty(TRUST_STORE_KEY));
    }

    public void loadProperty(String key) {
        Properties systemProperties = System.getProperties();
        if (systemProperties.get(key) == null) {
            String value = environment.getRequiredProperty(key);
            System.setProperty(key, environment.getRequiredProperty(key));
            log.debug("Property {} settes til {}", key, value);
        }
    }

}
