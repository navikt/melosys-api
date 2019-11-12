package no.nav.melosys.tjenester.gui.saksflyt;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.unntak.AnmodningUnntakService;
import no.nav.melosys.tjenester.gui.RestTjeneste;
import no.nav.melosys.tjenester.gui.dto.AnmodningUnntakDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"saksflyt", "anmodningsperioder"})
@Path("/saksflyt/anmodningsperioder")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class AnmodningUnntakTjeneste extends RestTjeneste {
    private final AnmodningUnntakService anmodningUnntakService;
    private final TilgangService tilgangService;

    @Autowired
    public AnmodningUnntakTjeneste(AnmodningUnntakService anmodningUnntakService, TilgangService tilgangService) {
        this.anmodningUnntakService = anmodningUnntakService;
        this.tilgangService = tilgangService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("{behandlingID}/bestill")
    @ApiOperation(value = "Anmodning om unntak for en gitt behandling")
    public Response anmodningOmUnntak(@PathParam("behandlingID") long behandlingID, AnmodningUnntakDto anmodningUnntakDto) throws MelosysException {
        tilgangService.sjekkTilgang(behandlingID);
        anmodningUnntakService.anmodningOmUnntak(behandlingID, anmodningUnntakDto.getMottakerinstitusjon());
        return Response.ok().build();
    }

    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("{behandlingID}/svar")
    @ApiOperation(value = "Sender et svar på anmodning om unntak basert på AnmodningsperiodeSvar som er registrert på behandlingen")
    public Response svar(@PathParam("behandlingID") long behandlingID) throws MelosysException {
        tilgangService.sjekkTilgang(behandlingID);
        anmodningUnntakService.anmodningOmUnntakSvar(behandlingID);
        return Response.ok().build();
    }
}
