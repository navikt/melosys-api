package no.nav.melosys.tjenester.gui.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["NAIS_CLUSTER_NAME"], havingValue = "dev-fss", matchIfMissing = true)
class OpenAPIConfig {

    @Bean
    fun customOpenAPI(): OpenAPI {
        return OpenAPI()
            .info(Info()
                .title("Melosys API")
                .description("Frontend API for Melosys")
                .version("1")
                .contact(Contact()
                    .name("teammelosys")
                    .email(null)
                    .url(null)))
            .components(Components()
                .addSecuritySchemes("JWT",
                    SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .`in`(SecurityScheme.In.HEADER)
                        .name("Authorization")))
            .addSecurityItem(SecurityRequirement().addList("JWT"))
    }
}
