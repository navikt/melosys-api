package no.nav.melosys;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import no.nav.vedtak.isso.OpenAMHelper;
import no.nav.vedtak.sts.client.SecurityConstants;

@SpringBootApplication
@PropertySource("classpath:integrasjon.properties")
@PropertySource("classpath:saksflyt.properties")
public class Application extends SpringBootServletInitializer implements EnvironmentAware {

    private static final Logger log = LoggerFactory.getLogger(Application.class);

    private static Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
        loadSystemProperties();
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
    private static void loadSystemProperties() {
        List<String> list = new ArrayList<>();

        // Til StsConfigurationUtil
        list.add(SecurityConstants.STS_URL_KEY);
        list.add(SecurityConstants.SYSTEMUSER_USERNAME);
        list.add(SecurityConstants.SYSTEMUSER_PASSWORD);

        // Til OpenAMHelper
        list.add(OpenAMHelper.ISSO_ISSUER_URL);
        list.add(OpenAMHelper.ISSO_RP_USER_USERNAME);

        list.forEach(s -> System.setProperty(s, environment.getRequiredProperty(s)));
    }

}