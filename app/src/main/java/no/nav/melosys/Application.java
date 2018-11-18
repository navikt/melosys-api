package no.nav.melosys;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

@ServletComponentScan("no.nav.melosys.integrasjon.felles")
@PropertySource(value = "classpath:saksflyt.properties", encoding = "utf-8")
@PropertySource(value = "classpath:service.properties", encoding = "utf-8")
@SpringBootApplication
@Controller
public class Application {

    public static void main(String[] args) {
        settSysteminnstillingerForTls();
        SpringApplication.run(Application.class, args);
    }

    // Kan skilles ut en Spring-profil for lokal utvikling, i form av en
    // application lifecycle event listener eller liknende ved behov. Akkurat
    // nå er det unødvendig, da default-initialiseringen ikke er i konflikt
    // med noe i produksjon.
    private static void settSysteminnstillingerForTls() {
        System.setProperty("javax.net.ssl.trustStore",
                System.getProperty("javax.net.ssl.trustStore",
                        "../target/nav_truststore_nonproduction_ny2.jts"));
        if (System.getProperty("javax.net.ssl.trustStorePassword") == null) {
            throw new IllegalStateException("Husk å sette systemegenskapen "
                    + "javax.net.ssl.trustStorePassword. Verdien kan hentes"
                    + " på Fasit.");
        }
    }

    @Bean
    public WebMvcConfigurerAdapter dispatcherServletConfigurer() {
        return new WebMvcConfigurerAdapter() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                registry.addResourceHandler("/frontendlogger/**").addResourceLocations("classpath:/frontendlogger/");
            }
        };
    }

    @GetMapping(path = { "/journalforing/**", "/sok/**" })
    public String forward() {
        return "forward:/";
    }
}