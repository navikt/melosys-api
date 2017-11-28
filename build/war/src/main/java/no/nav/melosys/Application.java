package no.nav.melosys;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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

import no.nav.vedtak.isso.OpenAMHelper;
import no.nav.vedtak.sts.client.SecurityConstants;

@ServletComponentScan("no.nav.melosys.integrasjon.felles")
@SpringBootApplication
@PropertySource(value = "classpath:integrasjon.properties", encoding = "utf-8")
@PropertySource(value = "classpath:saksflyt.properties", encoding = "utf-8")
@PropertySource(value = "classpath:service.properties", encoding = "utf-8")
public class Application extends SpringBootServletInitializer implements EnvironmentAware {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private static Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        Application.environment = environment;
        loadProperties();
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(Application.class, args);
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
        return builder.sources(Application.class);
    }

    // TODO Sikkerhet fra Foreldrepenger trenger system properties.
    // De settes her for nå til utvikling. Må settes i app-config også.
    private static void loadProperties() {
        List<String> list = new ArrayList<>();

        // Til StsConfigurationUtil
        list.add(SecurityConstants.STS_URL_KEY);
        list.add(SecurityConstants.SYSTEMUSER_USERNAME);
        list.add(SecurityConstants.SYSTEMUSER_PASSWORD);

        // Til OpenAMHelper
        list.add(OpenAMHelper.ISSO_ISSUER_URL);
        list.add(OpenAMHelper.ISSO_RP_USER_USERNAME);

        // Til LdapInnlogging
        list.add("ldap.url");
        list.add("ldap.username");
        list.add("ldap.domain");
        list.add("ldap.password");

        // LdapBrukeroppslag
        list.add("ldap.user.basedn");

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