package no.nav.melosys.tjenester.gui.saksflyt;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
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
    private final BehandlingRepository behandlingRepository;

    @Autowired
    public UnntakTjeneste(UnntaksperiodeService unntaksperiodeService, BehandlingRepository behandlingRepository) {
        this.unntaksperiodeService = unntaksperiodeService;
        this.behandlingRepository = behandlingRepository;
    }

    @POST
    @Path("{behandlingID}/ikkegodkjenn")
    public Response ikkeGodkjennUnntaksperiode(@PathParam("behandlingID") Long behandlingId, @ApiParam("vurderUnntaksperiodeDto") VurderUnntaksperiodeDto vurderUnntaksperiodeDto) throws FunksjonellException, TekniskException {
        Behandling behandling = hentOgValiderBehandlingsTypeUnntak(behandlingId);
        unntaksperiodeService.ikkeGodkjennPeriode(behandling, vurderUnntaksperiodeDto.getIkkeGodkjentBegrunnelseKoder(), vurderUnntaksperiodeDto.getBegrunnelseFritekst());
        return Response.noContent().build();
    }


    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{behandlingID}/godkjenn")
    public Response godkjennUnntaksperiode(@PathParam("behandlingID") Long behandlingId) throws FunksjonellException, TekniskException {
        Behandling behandling = hentOgValiderBehandlingsTypeUnntak(behandlingId);
        unntaksperiodeService.godkjennPeriode(behandling);
        return Response.noContent().build();
    }

    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("/{behandlingID}/innhentinfo")
    public Response innhentInformasjonUnntaksperiode(@PathParam("behandlingID") Long behandlingId) throws IkkeFunnetException {
        Behandling behandling = hentOgValiderBehandlingsTypeUnntak(behandlingId);
        unntaksperiodeService.behandlingUnderAvklaring(behandling);
        return Response.noContent().build();
    }

    private Behandling hentOgValiderBehandlingsTypeUnntak(long behandlingId) throws IkkeFunnetException {
        Behandling behandling = behandlingRepository.findById(behandlingId)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke behandling med id" + behandlingId));

        if (behandling.getType() != Behandlingstyper.UNNTAK_FRA_MEDLEMSKAP) {
            throw new BadRequestException("Behandling er ikke av type UNNTAK_FRA_MEDLEMSKAP");
        } else if (behandling.getStatus() != Behandlingsstatus.UNDER_BEHANDLING) {
            throw new BadRequestException("Behandling har status " + behandling.getStatus());
        }

        return behandling;
    }
}