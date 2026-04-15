package no.nav.melosys;

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation;
import org.springframework.boot.web.server.servlet.context.ServletComponentScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@ServletComponentScan("no.nav.melosys.integrasjon.felles")
@EnableJwtTokenValidation(ignore = {"org.springframework", "org.springdoc"})
@EnableCaching
@EnableRetry
public class ApplicationConfig {
}
