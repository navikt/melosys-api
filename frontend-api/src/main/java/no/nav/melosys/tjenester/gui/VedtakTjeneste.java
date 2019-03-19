package no.nav.melosys.tjenester.gui;

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
import no.nav.melosys.service.vedtak.VedtakService;
import no.nav.melosys.tjenester.gui.dto.EndreVedtakDto;
import no.nav.melosys.tjenester.gui.dto.FattVedtakDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = { "vedtak" })
@Path("/vedtak")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class VedtakTjeneste extends RestTjeneste {

    private final VedtakService vedtakService;

    private final Tilgang tilgang;

    @Autowired
    public VedtakTjeneste(VedtakService vedtakService, Tilgang tilgang) {
        this.vedtakService = vedtakService;
        this.tilgang = tilgang;
    }

    @POST
    @Path("{behandlingID}")
    @ApiOperation(value = "Fatter et vedtak for en gitt behandling")
    public Response fattVedtak(@PathParam("behandlingID") long behandlingID, @ApiParam("fattVedtakDto") FattVedtakDto fattVedtakDto) throws FunksjonellException, TekniskException {
        if (fattVedtakDto == null || fattVedtakDto.getBehandlingsresultattype() == null) {
            throw new BadRequestException();
        }

        tilgang.sjekk(behandlingID);
        if (fattVedtakDto.getBehandlingsresultattype() == Behandlingsresultattyper.ANMODNING_OM_UNNTAK) {
            vedtakService.anmodningOmUnntak(behandlingID);
        } else {
            vedtakService.fattVedtak(behandlingID, fattVedtakDto.getBehandlingsresultattype());
        }
        return Response.ok().build();
    }

    @POST
    @Path("/endre/{behandlingID}")
    @ApiOperation(value = "Endrer et vedtak for en gitt behandling")
    public Response endreVedtak(@PathParam("behandlingID") long behandlingID, @ApiParam("endreVedtakDto") EndreVedtakDto endreVedtakDto) throws FunksjonellException, TekniskException {
        if (endreVedtakDto.getBegrunnelseKode() == null) {
            throw new BadRequestException("Mangler BegrunnelseKode");
        }
        tilgang.sjekk(behandlingID);

        vedtakService.endreVedtak(behandlingID, endreVedtakDto.getBegrunnelseKode());

        return Response.ok().build();
    }
}