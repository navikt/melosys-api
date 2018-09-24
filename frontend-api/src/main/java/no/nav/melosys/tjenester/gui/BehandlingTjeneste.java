package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import no.nav.melosys.service.BehandlingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(tags = { "behandlinger" })
@Path("/behandlinger")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
@Transactional
public class BehandlingTjeneste extends RestTjeneste {

    private static final Logger log = LoggerFactory.getLogger(BehandlingTjeneste.class);

    private final BehandlingService behandlingerService;

    @Autowired
    public BehandlingTjeneste(BehandlingService behandlingerService) {
        this.behandlingerService = behandlingerService;
    }

    @GET
    @Path("status/{id}")
    @ApiOperation(value = "Status på saksopplysingoppfriskning basert på behandlingsid", notes = ("Returnerer status på oppfriskning av saksopplysing basert på behandlingsid."))
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
