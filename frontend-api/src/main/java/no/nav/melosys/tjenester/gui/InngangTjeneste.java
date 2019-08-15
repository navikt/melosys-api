package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.tjenester.gui.util.JsonResourceLoader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"inngangsvilkår"})
@Path("/inngangsvilkaar")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class InngangTjeneste extends RestTjeneste {
    private String jsonInngang;

    @Autowired
    public InngangTjeneste(ResourceLoader resourceLoader) throws IOException {
        jsonInngang = JsonResourceLoader.load(resourceLoader, "inngang.json");
    }

    @GET
    @Path("{saksnr}")
    @ApiOperation(value = "Henter en inngangsvurdering med et gitt saksnummer", response = String.class)
    public Response hentInngang(@ApiParam @PathParam("saksnr") String saksnummer) {
        return Response.ok().entity(jsonInngang).build();
    }
}
