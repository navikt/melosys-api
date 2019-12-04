package no.nav.melosys.tjenester.gui.saksflyt;

import javax.ws.rs.*;
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
        if (fattVedtakDto == null || fattVedtakDto.getBehandlingsresultatTypeKode() == null || fattVedtakDto.getVedtakstype() == null) {
            throw new BadRequestException("BehandlingsresultatTypeKode eller vedtakstype mangler.");
        }
        tilgangService.sjekkTilgang(behandlingID);
        vedtakService.fattVedtak(behandlingID, fattVedtakDto.getBehandlingsresultatTypeKode(), fattVedtakDto.getFritekst(),
            fattVedtakDto.getMottakerinstitusjon(), fattVedtakDto.getVedtakstype(), fattVedtakDto.getRevurderBegrunnelse());
        return Response.ok().build();
    }

    @POST
    @Path("{behandlingID}/endre")
    @ApiOperation(value = "Endrer et vedtak for en gitt behandling")
    public Response endreVedtak(@PathParam("behandlingID") long behandlingID, @ApiParam("endreVedtakDto") EndreVedtakDto endreVedtakDto)
        throws FunksjonellException, TekniskException {
        if (endreVedtakDto.getBegrunnelseKode() == null) {
            throw new BadRequestException("BegrunnelseKode mangler.");
        }
        if (endreVedtakDto.getBehandlingstype() == null) {
            throw new BadRequestException("Behandlingstype mangler.");
        }
        tilgangService.sjekkTilgang(behandlingID);
        vedtakService.endreVedtak(behandlingID, endreVedtakDto.getBegrunnelseKode(), endreVedtakDto.getBehandlingstype(), endreVedtakDto.getFritekst());
        return Response.ok().build();
    }


    @POST
    @Path("{behandlingID}/revurder")
    @ApiOperation(value = "Korrigerer eller omgjør vedtak for en sak ved å opprette en ny behandling basert på en eksisterende")
    public Response revurderVedtak(@PathParam("behandlingID") long behandlingID) throws FunksjonellException, TekniskException {
        tilgangService.sjekkTilgang(behandlingID);

        long nyBehandlingID = vedtakService.revurderVedtak(behandlingID);
        return Response.ok(nyBehandlingID).build();
    }
}