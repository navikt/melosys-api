package no.nav.melosys.tjenester.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class RestControllerInterceptorConfiguration implements WebMvcConfigurer  {

    private final Logger logger = LoggerFactory.getLogger(RestControllerInterceptorConfiguration.class);
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(getRestControllerInterceptor());
    }

    @Bean
    public RestControllerInterceptor getRestControllerInterceptor() {
        logger.info("Registering RestControllerInterceptor");
        return new RestControllerInterceptor();
    }

}
