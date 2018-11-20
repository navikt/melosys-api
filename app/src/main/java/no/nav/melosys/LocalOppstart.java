package no.nav.melosys;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@Profile("local")
public class LocalOppstart implements Oppstart, EnvironmentAware {

    private static final Logger log = LoggerFactory.getLogger(LocalOppstart.class);

    private static Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        LocalOppstart.environment = environment;
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
