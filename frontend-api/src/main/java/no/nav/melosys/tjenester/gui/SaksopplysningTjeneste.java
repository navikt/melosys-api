package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.*;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.SaksopplysningerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static javax.ws.rs.core.Response.Status.NO_CONTENT;

@Api(tags = { "saksopplysninger" })
@Path("/saksopplysninger")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
@Transactional
public class SaksopplysningTjeneste extends RestTjeneste {

    private static final Logger log = LoggerFactory.getLogger(SaksopplysningTjeneste.class);

    private final SaksopplysningerService saksopplysningerService;

    @Autowired
    public SaksopplysningTjeneste(SaksopplysningerService saksopplysningerService) {
        this.saksopplysningerService = saksopplysningerService;
    }

    @GET
    @Path("oppfrisk/{id}")
    @ApiOperation(value = "Oppfrisker saksopplysing basert på behandlingsid", notes = ("Oppfrisker saksopplysing basert på behandlingsid."))
    @ApiResponses(value = {
        @ApiResponse(code = 204, message = "No Content"),
        @ApiResponse(code = 404, message = "Behandling ikke funnet"),
        @ApiResponse(code = 500, message = "Uventet teknisk Feil")
    })
    public Response oppfriskSaksopplysning(@PathParam("id") @ApiParam("behandlingsid.") long id) {

        try {
            saksopplysningerService.oppfriskSaksopplysning(id);
            return Response.status(NO_CONTENT).build();
        } catch (IkkeFunnetException e) {
            log.error("Behandling ikke funnet", e);
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (TekniskException e) {
            log.error("Uventet teknisk Feil", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
