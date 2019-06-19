package no.nav.melosys.tjenester.gui.saksflyt;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.unntak.AnmodningUnntakService;
import no.nav.melosys.tjenester.gui.RestTjeneste;
import no.nav.melosys.tjenester.gui.dto.FattVedtakDto;
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
    private final Tilgang tilgang;

    @Autowired
    public AnmodningUnntakTjeneste(AnmodningUnntakService anmodningUnntakService, Tilgang tilgang) {
        this.anmodningUnntakService = anmodningUnntakService;
        this.tilgang = tilgang;
    }

    @POST
    @Path("{behandlingID}")
    @ApiOperation(value = "Anmodning om unntak for en gitt behandling")
    public Response anmodningOmUnntak(@PathParam("behandlingID") long behandlingID, @ApiParam("fattVedtakDto") FattVedtakDto fattVedtakDto) throws FunksjonellException, TekniskException {
        if (fattVedtakDto == null || fattVedtakDto.getBehandlingsresultattype() != Behandlingsresultattyper.ANMODNING_OM_UNNTAK) {
            throw new BadRequestException();
        }
        tilgang.sjekk(behandlingID);
        anmodningUnntakService.anmodningOmUnntak(behandlingID);
        return Response.ok().build();
    }
}
