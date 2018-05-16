package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

@Api(tags = {"inngang"})
@Path("/inngang")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class InngangTjeneste extends RestTjeneste {
    private String jsonInngang;

    @Autowired
    public InngangTjeneste(ResourceLoader resourceLoader) throws IOException {
        Resource resource = resourceLoader.getResource("classpath:inngang.json");

        InputStream inputStream = resource.getInputStream();

        StringBuilder stringBuilder = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader
                (inputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                stringBuilder.append((char) c);
            }
        }

        jsonInngang = stringBuilder.toString();
    }

    @GET
    @Path("{saksnr}")
    @ApiOperation(value = "Henter en inngangsvurdering med et gitt saksnummer", notes = ("Spesifikke saker kan hentes via saksnummer."))
    public Response hentInngang(@PathParam("saksnr") @ApiParam("Saksnummer.") String saksnummer) {
        return Response.ok().entity(jsonInngang).build();
    }
}
