package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.service.BehandlingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = { "behandlinger" })
@Path("/behandlinger")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
@Transactional
public class BehandlingTjeneste extends RestTjeneste {

    private final BehandlingService behandlingerService;

    @Autowired
    public BehandlingTjeneste(BehandlingService behandlingerService) {
        this.behandlingerService = behandlingerService;
    }

    @GET
    @Path("{id}/status")
    @ApiOperation(value = "Status på behandling", notes = ("Returnerer status (Progress/Done) på behandling"), response = String.class)
    public Response statusBehandling(@PathParam("id") @ApiParam("behandlingsid.") long id) {
        String status;
        if (behandlingerService.aktivProsessinstansEksistererFor(id)) {
            status = "PROGRESS";
        } else {
            status = "DONE";
        }
        return Response.ok(status).build();
    }
}
