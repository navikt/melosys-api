package no.nav.melosys.tjenester.gui;

import java.io.IOException;
import java.util.Random;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
        boolean feilet = new Random().nextBoolean();
        String filename = feilet ? "bostedavklaring_feilmelding.json" : "bostedavklaring.json";
        jsonBostedAvklaring = JsonResourceLoader.load(resourceLoader, filename);
    }

    @GET
    @Path("{behandlingID}")
    @ApiOperation(value = "Henter faktaavklaring for en gitt søknad", response = String.class)
    public Response hentFaktaavklaring(@ApiParam @PathParam("behandlingID") long behandlingID) {
        // TODO Mock. Venter på avklaringer
        return Response.ok().entity(jsonFaktaAvklaring).build();
    }

    @POST
    @Path("{behandlingID}")
    @ApiOperation(value = "Lagrer faktaavklaring for en gitt søknad", response = String.class)
    public Response utførFaktaavklaring(@ApiParam @PathParam("behandlingID") long behandlingID, @ApiParam String body) {
        // TODO Mock. Venter på avklaringer
        return Response.ok().entity(body).build();
    }

    @GET
    @Path("bosted/{behandlingID}")
    @ApiOperation(value = "Henter bostedavklaring for et gitt bosted", response = String.class)
    public Response hentBostedAvklaring(@ApiParam @PathParam("behandlingID") long behandlingID) {
        // TODO Mock. Venter på avklaringer
        return Response.ok().entity(jsonBostedAvklaring).build();
    }
}
