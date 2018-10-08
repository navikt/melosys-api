package no.nav.melosys.tjenester.gui.config;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Resource;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import springfox.documentation.swagger.web.InMemorySwaggerResourcesProvider;
import springfox.documentation.swagger.web.SwaggerResource;
import springfox.documentation.swagger.web.SwaggerResourcesProvider;

@Component
@Primary
public class CombinedResourceProvider implements SwaggerResourcesProvider {

    @Resource
    private InMemorySwaggerResourcesProvider inMemorySwaggerResourcesProvider;

    public List<SwaggerResource> get() {

        SwaggerResource jerseySwaggerResource = new SwaggerResource();
        jerseySwaggerResource.setLocation("/api/swagger.json");
        jerseySwaggerResource.setSwaggerVersion("2.0");
        jerseySwaggerResource.setName("Jersey");

        return Stream.concat(Stream.of(jerseySwaggerResource),
            inMemorySwaggerResourcesProvider.get().stream()).collect(Collectors.toList());
    }

}