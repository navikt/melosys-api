package no.nav.melosys.sikkerhet.cors;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
@ConditionalOnProperty(name = "cors.allowOrigin")
public class CorsConfig {

    private final List<String> allowedOrigins;

    @Autowired
    public CorsConfig(@Value("${cors.allowOrigin}") String... allowedOrigins) {
        this.allowedOrigins = Arrays.asList(allowedOrigins);
    }

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.addAllowedMethod("POST, PUT, GET, OPTIONS, DELETE");
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        config.addAllowedHeader("Content-type");
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
  