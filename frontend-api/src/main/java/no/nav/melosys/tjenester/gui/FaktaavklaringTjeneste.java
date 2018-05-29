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
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import java.io.IOException;

import no.nav.melosys.tjenester.gui.util.JsonResourceLoader;

@Api(tags = {"faktaavklaring"})
@Path("/faktaavklaring")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class FaktaavklaringTjeneste extends RestTjeneste {

    private String jsonFaktaAvklaring;
    private String jsonBostedAvklaring;

    @Autowired
    public FaktaavklaringTjeneste(ResourceLoader resourceLoader) throws IOException {
        jsonFaktaAvklaring = JsonResourceLoader.load(resourceLoader, "faktaavklaring.json");
        int i = JsonResourceLoader.getRandomNumberInRange(0, 1);
        String filename = (i == 0) ? "bostedavklaring_feilmelding.json" : "bostedavklaring.json";
        jsonBostedAvklaring = JsonResourceLoader.load(resourceLoader, filename);
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

    @GET
    @Path("bosted/{behandlingID}")
    @ApiOperation(value = "Henter bostedavklaring for et gitt bosted")
    public Response hentBostedAvklaring(@PathParam("behandlingID") long behandlingID) {
        // TODO Mock. Venter på avklaringer
        return Response.ok().entity(jsonBostedAvklaring).build();
    }
}
