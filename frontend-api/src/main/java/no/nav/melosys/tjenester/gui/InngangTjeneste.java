package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import java.io.IOException;

import no.nav.melosys.tjenester.gui.util.JsonResourceLoader;

@Api(tags = {"inngang"})
@Path("/inngang")
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
    @ApiOperation(value = "Henter en inngangsvurdering med et gitt saksnummer")
    public Response hentInngang(@PathParam("saksnr") @ApiParam("Saksnummer.") String saksnummer) {
        return Response.ok().entity(jsonInngang).build();
    }
}
