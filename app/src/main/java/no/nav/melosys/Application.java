package no.nav.melosys;

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@ServletComponentScan("no.nav.melosys.integrasjon.felles")
@PropertySource(value = "classpath:service.properties", encoding = "utf-8")
@SpringBootApplication
@Controller
@EnableJwtTokenValidation(ignore={"org.springframework", "springfox.documentation"})
@EnableRetry
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @GetMapping(path = { "/journalforing/**", "/sok/**" })
    public String forward() {
        return "forward:/";
    }
}