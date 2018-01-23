package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Api(tags = {"faktaavklaring"})
@Path("/faktaavklaring")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class FaktaavklaringTjeneste extends RestTjeneste {

    @GET
    @Path("{behandlingID}")
    @ApiOperation(value = "Utfører faktaavklaring på en gitt søknad")
    public Response utførFaktaavklaring(@PathParam("behandlingID") long behandlingID) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }
}
