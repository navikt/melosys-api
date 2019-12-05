package no.nav.melosys.tjenester.gui.config;

import java.util.Collections;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableSwagger2
@ConditionalOnProperty(name = "NAIS_CLUSTER_NAME", havingValue = "dev-fss", matchIfMissing = true)
public class SwaggerConfig {

    @Bean
    public Docket api() {
        //TODO: SecurityScheme/SecurityContext
        return new Docket(DocumentationType.SWAGGER_2)
            .apiInfo(apiInfo())
            .select()
            .apis(RequestHandlerSelectors.basePackage("no.nav.melosys.tjenester.gui"))
            .paths(PathSelectors.any())
            .build()
            .enable(true);
    }

    private ApiInfo apiInfo() {
        return new ApiInfo(
            "Melosys API",
            "Frontend API for Melosys",
            "1",
            null,
            new Contact("teammelosys", null, null),
            null, null, Collections.emptyList()
        );
    }
}
