package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.*;
import java.nio.file.Files;

@Api(tags = {"landkoder"})
@Path("/landkoder")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
@Transactional
public class LandkoderTjeneste extends RestTjeneste {

    private String jsonLandkoder;

    @Autowired
    public LandkoderTjeneste(ResourceLoader resourceLoader) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:landkoder.json");
        jsonLandkoder = new String(Files.readAllBytes(resource.getFile().toPath()));
    }

    @GET
    public String getLandkoder() {
        return jsonLandkoder;
    }
}
