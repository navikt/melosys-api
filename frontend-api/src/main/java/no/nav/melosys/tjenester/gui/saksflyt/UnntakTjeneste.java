package no.nav.melosys.tjenester.gui.saksflyt;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.unntaksperiode.UnntaksperiodeService;
import no.nav.melosys.tjenester.gui.RestTjeneste;
import no.nav.melosys.tjenester.gui.dto.VurderUnntaksperiodeDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"saksflyt", "unntaksperioder"})
@Path("/saksflyt/unntaksperioder")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class UnntakTjeneste extends RestTjeneste {

    private final UnntaksperiodeService unntaksperiodeService;

    @Autowired
    public UnntakTjeneste(UnntaksperiodeService unntaksperiodeService) {
        this.unntaksperiodeService = unntaksperiodeService;
    }

    @POST
    @Path("{behandlingID}/ikkegodkjenn")
    public Response ikkeGodkjennUnntaksperiode(@PathParam("behandlingID") Long behandlingId, @ApiParam("vurderUnntaksperiodeDto") VurderUnntaksperiodeDto vurderUnntaksperiodeDto) throws FunksjonellException, TekniskException {
        unntaksperiodeService.ikkeGodkjennPeriode(behandlingId, vurderUnntaksperiodeDto.getIkkeGodkjentBegrunnelseKoder(), vurderUnntaksperiodeDto.getBegrunnelseFritekst());
        return Response.noContent().build();
    }

    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{behandlingID}/godkjenn")
    public Response godkjennUnntaksperiode(@PathParam("behandlingID") Long behandlingId) throws FunksjonellException, TekniskException {
        unntaksperiodeService.godkjennPeriode(behandlingId);
        return Response.noContent().build();
    }

    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{behandlingID}/innhentinfo")
    public Response innhentInformasjonUnntaksperiode(@PathParam("behandlingID") Long behandlingId) throws FunksjonellException {
        unntaksperiodeService.behandlingUnderAvklaring(behandlingId);
        return Response.noContent().build();
    }
}