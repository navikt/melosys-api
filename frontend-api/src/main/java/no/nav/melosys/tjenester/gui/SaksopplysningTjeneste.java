package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.*;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.SaksopplysningerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import static javax.ws.rs.core.Response.Status.NO_CONTENT;

@Api(tags = { "saksopplysninger" })
@Path("/saksopplysninger")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class SaksopplysningTjeneste extends RestTjeneste {

    private final SaksopplysningerService saksopplysningerService;

    @Autowired
    public SaksopplysningTjeneste(SaksopplysningerService saksopplysningerService) {
        this.saksopplysningerService = saksopplysningerService;
    }

    @GET
    @Path("oppfriskning/{behandlingID}")
    @ApiOperation(value = "Oppfrisker saksopplysing basert på behandlingsid", notes = ("Oppfrisker saksopplysing basert på behandlingsid."))
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "No Content"),
        @ApiResponse(code = 404, message = "Behandling ikke funnet"),
        @ApiResponse(code = 500, message = "Uventet teknisk Feil")
    })
    public Response oppfriskSaksopplysning(@PathParam("behandlingID") @ApiParam("behandlingsid.") long id) throws IkkeFunnetException, TekniskException {
        saksopplysningerService.oppfriskSaksopplysning(id);
        return Response.status(NO_CONTENT).build();
    }

    @GET
    @Path("oppfriskning/{behandlingID}/status")
    @ApiOperation(value = "Status på oppfrisking av behandling ", notes = ("Returnerer status (Progress/Done) på oppfrisking av behandling"))
    public Response hentOppfriskingStatusForBehandling(@ApiParam @PathParam("behandlingID") long behandlingID) {
        String status;
        if (saksopplysningerService.harAktivOppfrisking(behandlingID)) {
            status = "\"PROGRESS\"";
        } else {
            status = "\"DONE\"";
        }
        return Response.ok(status).build();
    }
}
