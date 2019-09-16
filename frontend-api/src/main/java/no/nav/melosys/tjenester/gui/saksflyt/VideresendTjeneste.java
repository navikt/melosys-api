package no.nav.melosys.tjenester.gui.saksflyt;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.VideresendService;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.tjenester.gui.RestTjeneste;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"saksflyt", "soknad"})
@Path("/saksflyt/soknader")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class VideresendTjeneste extends RestTjeneste {
    private final TilgangService tilgangService;
    private final VideresendService videresendService;

    @Autowired
    public VideresendTjeneste(TilgangService tilgangService, VideresendService videresendService) {
        this.tilgangService = tilgangService;
        this.videresendService = videresendService;
    }

    @PUT
    @Path("{behandlingID}/videresend")
    @ApiOperation(value = "Videresender søknad for en gitt behandling")
    public Response videresend(@PathParam("behandlingID") long behandlingID) throws FunksjonellException, TekniskException {
        tilgangService.sjekkTilgang(behandlingID);
        videresendService.videresend(behandlingID);
        return Response.ok().build();
    }
}