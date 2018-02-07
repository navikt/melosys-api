package no.nav.melosys;

import no.nav.modig.testcertificates.TestCertificates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@ServletComponentScan("no.nav.melosys.integrasjon.felles")
@SpringBootApplication
@PropertySource(value = "classpath:saksflyt.properties", encoding = "utf-8")
@PropertySource(value = "classpath:service.properties", encoding = "utf-8")
public class Application extends SpringBootServletInitializer implements EnvironmentAware {

    private static final String TRUST_STORE = "javax.net.ssl.trustStore";
    private static final String TRUST_STORE_PWD = "javax.net.ssl.trustStorePassword";

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private static Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        Application.environment = environment;

        // TODO Fjerne. For å kunne kjøre lokalt:
        loadProperties();
        configureSsl();
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(Application.class);
    }

    private void configureSsl() {
        String trustStore = System.getenv(TRUST_STORE);
        if (trustStore != null) {
            System.setProperty(TRUST_STORE, trustStore);
            log.info("Property {} satt til {}", TRUST_STORE, trustStore);

            String trustStorePwd = System.getenv(TRUST_STORE_PWD);
            if (trustStorePwd != null) {
                System.setProperty(TRUST_STORE_PWD, trustStorePwd);
            } else {
                throw new IllegalStateException("Property " + TRUST_STORE_PWD + " mangler.");
            }

        } else {
            // lokalt
            log.debug("Property {} ikke satt.", TRUST_STORE);
            log.info("Test sertifikater brukes.");
            TestCertificates.setupKeyAndTrustStore();
        }
    }

    // Sikkerhet trenger system properties.
    private static void loadProperties() {
        List<String> list = new ArrayList<>();

        // Til StsConfigurationUtil
        list.add("securityTokenService.url");
        list.add("systemuser.username");
        list.add("systemuser.password");

        list.forEach(key -> loadProperty(key));
    }

    private static void loadProperty(String key) {
        Properties systemProperties = System.getProperties();
        if (systemProperties.get(key) == null) {
            String value = environment.getRequiredProperty(key);
            System.setProperty(key, environment.getRequiredProperty(key));
            log.debug("Property {} settes til {}", key, value);
        }
    }

}