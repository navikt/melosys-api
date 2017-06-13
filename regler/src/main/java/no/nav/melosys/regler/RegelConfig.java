package no.nav.melosys.regler;

import javax.annotation.PostConstruct;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.wadl.internal.WadlResource;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

//import io.swagger.annotations.Api;
import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jaxrs.listing.ApiListingResource;
import io.swagger.jaxrs.listing.SwaggerSerializers;
import no.nav.melosys.regler.service.lovvalg.LovvalgTjenesteImpl;

@Configuration
@ComponentScan
@EnableAutoConfiguration
public class RegelConfig extends ResourceConfig {

    /*
     * FIXME (farjam 2017-06-06): En smule låst til Jersey... 
     */

    public RegelConfig() {
        register(LovvalgTjenesteImpl.class);
        register(WadlResource.class);
    }

    @PostConstruct
    public void configSwagger() {
        register(ApiListingResource.class);
        register(SwaggerSerializers.class);
    }
    
    @Bean
    /** BeanConfig hentes av swagger */
    public BeanConfig getBeanConfig() {
        BeanConfig config = new BeanConfig();
        config.setConfigId("melosys-regler");
        config.setTitle("Melosys regler");
        config.setVersion("0");
        config.setContact("Team Melosys");
        config.setSchemes(new String[] {"http", "https"});
        config.setBasePath("/");
        config.setResourcePackage("no.nav.melosys.regler");
        config.setPrettyPrint(true);
        config.setScan(true);
        return config;
    }

}
