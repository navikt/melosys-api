package no.nav.melosys.tjenester.gui;

import java.util.Set;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.IkkeGodkjentBegrunnelser;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.unntaksperiode.UnntaksperiodeService;
import no.nav.melosys.service.vedtak.VedtakService;
import no.nav.melosys.tjenester.gui.dto.EndreVedtakDto;
import no.nav.melosys.tjenester.gui.dto.FattVedtakDto;
import no.nav.melosys.tjenester.gui.dto.VurderUnntaksperiodeDto;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = { "saksflyt" })
@Path("/saksflyt")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class SaksflytTjeneste extends RestTjeneste {

    private final VedtakService vedtakService;
    private final UnntaksperiodeService unntaksperiodeService;
    private final BehandlingRepository behandlingRepository;

    private final Tilgang tilgang;

    @Autowired
    public SaksflytTjeneste(VedtakService vedtakService, UnntaksperiodeService unntaksperiodeService, BehandlingRepository behandlingRepository, Tilgang tilgang) {
        this.vedtakService = vedtakService;
        this.unntaksperiodeService = unntaksperiodeService;
        this.behandlingRepository = behandlingRepository;
        this.tilgang = tilgang;
    }

    @POST
    @Path("/vedtak/{behandlingID}")
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
    @Path("/vedtak/endre/{behandlingID}")
    @ApiOperation(value = "Endrer et vedtak for en gitt behandling")
    public Response endreVedtak(@PathParam("behandlingID") long behandlingID, @ApiParam("endreVedtakDto") EndreVedtakDto endreVedtakDto) throws FunksjonellException, TekniskException {
        if (endreVedtakDto.getBegrunnelseKode() == null) {
            throw new BadRequestException("Mangler BegrunnelseKode");
        }
        tilgang.sjekk(behandlingID);

        vedtakService.endreVedtak(behandlingID, endreVedtakDto.getBegrunnelseKode());

        return Response.ok().build();
    }

    @POST
    @Path("/unntaksperiode/{behandlingID}/ikkegodkjenn")
    public Response ikkeGodkjennUnntaksperiode(@PathParam("behandlingID") Long behandlingId, @ApiParam("vurderUnntaksperiodeDto") VurderUnntaksperiodeDto vurderUnntaksperiodeDto) throws IkkeFunnetException {

        Behandling behandling = hentOgValiderBehandlingsTypeUnntak(behandlingId);
        validerBegrunnelser(vurderUnntaksperiodeDto.getIkkeGodkjentBegrunnelseKoder(), vurderUnntaksperiodeDto.getBegrunnelseFritekst());
        unntaksperiodeService.avvisPeriode(behandling, vurderUnntaksperiodeDto.getIkkeGodkjentBegrunnelseKoder(), vurderUnntaksperiodeDto.getBegrunnelseFritekst());

        return Response.ok().build();
    }


    @POST
    @Path("/unntaksperiode/{behandlingID}/godkjenn")
    public Response godkjennUnntaksperiode(@PathParam("behandlingID") Long behandlingId) throws IkkeFunnetException {
        Behandling behandling = hentOgValiderBehandlingsTypeUnntak(behandlingId);
        unntaksperiodeService.godkjennPeriode(behandling);
        return Response.ok().build();
    }

    @POST
    @Path("/unntaksperiode/{behandlingID}/innhentinfo")
    public Response innhentInformasjonUnntaksperiode(@PathParam("behandlingID") Long behandlingId) throws IkkeFunnetException {
        Behandling behandling = hentOgValiderBehandlingsTypeUnntak(behandlingId);
        unntaksperiodeService.behandlingUnderAvklaring(behandling);
        return Response.ok().build();
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

    private void validerBegrunnelser(Set<IkkeGodkjentBegrunnelser> begrunnelser, String fritekst) {
        if (begrunnelser.isEmpty()) {
            throw new BadRequestException("Ingen begrunnelser for avlag av periode");
        } else if (begrunnelser.contains(IkkeGodkjentBegrunnelser.ANNET) && StringUtils.isEmpty(fritekst)) {
            throw new BadRequestException("Begrunnelse " + IkkeGodkjentBegrunnelser.ANNET + " krever fritekst!");
        }
    }
}