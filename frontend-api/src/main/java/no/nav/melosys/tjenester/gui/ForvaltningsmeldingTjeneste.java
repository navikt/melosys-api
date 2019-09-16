package no.nav.melosys.tjenester.gui;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.ForvaltningsmeldingService;
import no.nav.melosys.service.abac.TilgangService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"forvaltningsmelding"})
@Path("/forvaltningsmelding")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class ForvaltningsmeldingTjeneste extends RestTjeneste {
    
    private final ForvaltningsmeldingService forvaltningsmeldingService;
    private final TilgangService tilgangService;

    @Autowired
    public ForvaltningsmeldingTjeneste(ForvaltningsmeldingService forvaltningsmeldingService, TilgangService tilgangService) {
        this.forvaltningsmeldingService = forvaltningsmeldingService;
        this.tilgangService = tilgangService;
    }

    @POST
    @Path("{behandlingID}")
    @ApiOperation(value = "Send forvaltningsmelding")
    public void sendForvaltningsmelding(@PathParam("behandlingID") long behandlingID) throws MelosysException {
        tilgangService.sjekkTilgang(behandlingID);
        forvaltningsmeldingService.sendForvaltningsmelding(behandlingID);
    }
}
