package no.nav.melosys.regler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.PropertySource;


/**
 * Spring konfigurasjon
 */
@SpringBootApplication
@PropertySource("classpath:regler.properties")
@EnableAutoConfiguration
public class ReglerApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
      return builder.sources(ReglerApplication.class);
    }
     
    public static void main(String[] args) {
        SpringApplication.run(ReglerApplication.class, args);
    }

}
