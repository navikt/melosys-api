package no.nav.melosys.tjenester.gui.saksflyt;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.vedtak.VedtakService;
import no.nav.melosys.tjenester.gui.RestTjeneste;
import no.nav.melosys.tjenester.gui.dto.EndreVedtakDto;
import no.nav.melosys.tjenester.gui.dto.FattVedtakDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"saksflyt", "vedtak"})
@Path("/saksflyt/vedtak")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class VedtakTjeneste extends RestTjeneste {
    private final VedtakService vedtakService;
    private final TilgangService tilgangService;

    @Autowired
    public VedtakTjeneste(VedtakService vedtakService, TilgangService tilgangService) {
        this.vedtakService = vedtakService;
        this.tilgangService = tilgangService;
    }

    @POST
    @Path("{behandlingID}/fatt")
    @ApiOperation(value = "Fatter et vedtak for en gitt behandling")
    public Response fattVedtak(@PathParam("behandlingID") long behandlingID, @ApiParam("fattVedtakDto") FattVedtakDto fattVedtakDto) throws MelosysException {
        if (fattVedtakDto == null || fattVedtakDto.getBehandlingsresultatTypeKode() == null) {
            throw new BadRequestException();
        }
        tilgangService.sjekkTilgang(behandlingID);
        vedtakService.fattVedtak(behandlingID, fattVedtakDto.getBehandlingsresultatTypeKode(), fattVedtakDto.getMottakerinstitusjon());
        return Response.ok().build();
    }

    @POST
    @Path("{behandlingID}/endreperiode")
    @ApiOperation(value = "Endrer et vedtak for en gitt behandling")
    public Response endreVedtak(@PathParam("behandlingID") long behandlingID, @ApiParam("endreVedtakDto") EndreVedtakDto endreVedtakDto) throws FunksjonellException, TekniskException {
        if (endreVedtakDto.getBegrunnelseKode() == null) {
            throw new BadRequestException("Mangler BegrunnelseKode");
        }
        tilgangService.sjekkTilgang(behandlingID);
        vedtakService.endreVedtak(behandlingID, endreVedtakDto.getBegrunnelseKode());
        return Response.ok().build();
    }
}