package no.nav.melosys.sikkerhet.abac;

import no.nav.abac.xacml.NavAttributter;
import no.nav.freg.abac.core.annotation.attribute.AttributeSupplier;
import no.nav.freg.abac.spring.config.AbacConfig;
import no.nav.freg.abac.spring.config.AbacRestTemplateConfig;
import no.nav.freg.abac.core.annotation.attribute.AbacAttributeLocator;
import no.nav.freg.abac.core.annotation.attribute.ResolvingAbacAttributeLocator;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;

import java.util.HashSet;
import java.util.Set;

@Configuration
@Import({
        AbacConfig.class,
        AbacRestTemplateConfig.class
})
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class AbacDefaultConfig {


    @Bean
    Set<String> abacDefaultEnvironment() {
        Set<String> values = new HashSet<>();
        values.add(NavAttributter.ENVIRONMENT_FELLES_PEP_ID);
        values.add(NavAttributter.ENVIRONMENT_FELLES_OIDC_TOKEN_BODY);
        return values;
    }

    @Bean
    Set<String> abacDefaultResources() {
        Set<String> values = new HashSet<>();
        values.add(NavAttributter.RESOURCE_FELLES_DOMENE);
        return values;
    }

    @Bean
    Set<String> abacDefaultSubjects() {
          return new HashSet<>();
    }

    @Bean
    AbacAttributeLocator pepIdLocator() {
        return new ResolvingAbacAttributeLocator(NavAttributter.ENVIRONMENT_FELLES_PEP_ID, () -> "melosys");
    }

    @Bean
    AbacAttributeLocator samlTokenLocator() {
        return new ResolvingAbacAttributeLocator(NavAttributter.ENVIRONMENT_FELLES_OIDC_TOKEN_BODY, new AttributeSupplier() {
            @Override
            public Object get() {
                String tokenBody = getOidcTokenBody();
                return tokenBody;
            }
        });
    }

    public String getOidcTokenBody() {
        String token = SubjectHandler.getInstance().getOidcTokenString();
        if (token == null) {
            return "";
        }

        final String[] tokenParts = token.split("\\.");
        return tokenParts.length == 1 ? tokenParts[0] : tokenParts[1];
    }

    @Bean
    AbacAttributeLocator fellesDomeneLocator() {
        return new ResolvingAbacAttributeLocator(NavAttributter.RESOURCE_FELLES_DOMENE, () -> "melosys");
    }
}
