package no.nav.melosys.tjenester.gui.config;

import java.util.Collections;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;

@Configuration
@ConditionalOnProperty(name = "NAIS_CLUSTER_NAME", havingValue = "dev-fss", matchIfMissing = true)
public class SwaggerConfig {

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
            .apiInfo(apiInfo())
            .select()
            .apis(RequestHandlerSelectors.basePackage("no.nav.melosys.tjenester.gui"))
            .paths(PathSelectors.any())
            .build()
            .securityContexts(Collections.singletonList(
                SecurityContext.builder()
                    .securityReferences(
                        Collections.singletonList(SecurityReference.builder()
                            .reference("JWT")
                            .scopes(new AuthorizationScope[0])
                            .build()
                        )
                    )
                    .build())
            )
            .securitySchemes(Collections.singletonList(new ApiKey("JWT", "Authorization", "header")))
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
