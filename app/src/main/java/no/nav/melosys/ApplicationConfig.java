package no.nav.melosys;

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@ServletComponentScan("no.nav.melosys.integrasjon.felles")
@PropertySource(value = "classpath:service.properties", encoding = "utf-8")
@EnableJwtTokenValidation(ignore = {"org.springframework", "springfox.documentation", "org.springdoc"})
@EnableCaching
@EnableRetry
public class ApplicationConfig {
}
