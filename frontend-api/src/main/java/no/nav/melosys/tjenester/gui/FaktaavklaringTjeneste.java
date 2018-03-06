package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Api(tags = {"faktaavklaring"})
@Path("/faktaavklaring")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class FaktaavklaringTjeneste extends RestTjeneste {

    private String jsonFaktaAvklaring;

    @Autowired
    public FaktaavklaringTjeneste(ResourceLoader resourceLoader) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:faktaavklaring.json");

        InputStream inputStream = resource.getInputStream();

        StringBuilder stringBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader
                (inputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                stringBuilder.append((char) c);
            }
        }

        jsonFaktaAvklaring = stringBuilder.toString();
    }

    @GET
    @Path("{behandlingID}")
    @ApiOperation(value = "Henter faktaavklaring for en gitt søknad")
    public Response hentFaktaavklaring(@PathParam("behandlingID") long behandlingID) {
        // TODO Mock. Venter på avklaringer
        return Response.ok().entity(jsonFaktaAvklaring).build();
    }

    @POST
    @Path("{behandlingID}")
    @ApiOperation(value = "Lagrer faktaavklaring for en gitt søknad")
    public Response utførFaktaavklaring(@PathParam("behandlingID") long behandlingID, String body) {
        // TODO Mock. Venter på avklaringer
        return Response.ok().entity(jsonFaktaAvklaring).build();
    }
}
