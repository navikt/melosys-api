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

@Api(tags = { "saksflyt" })
@Path("/saksflyt")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
@Transactional
public class SaksflytTjeneste extends RestTjeneste {

    private final BehandlingService behandlingerService;

    @Autowired
    public SaksflytTjeneste(BehandlingService behandlingerService) {
        this.behandlingerService = behandlingerService;
    }

    @GET
    @Path("status/{behandlingID}")
    @ApiOperation(value = "Status på oppfrisking av behandling ", notes = ("Returnerer status (Progress/Done) på oppfrisking av behandling"))
    public Response hentOppfriskingStatusForBehandling(@ApiParam @PathParam("behandlingID") long behandlingID) {
        String status;
        if (behandlingerService.harAktivOppfrisking(behandlingID)) {
            status = "PROGRESS";
        } else {
            status = "DONE";
        }
        return Response.ok(status).build();
    }
}
