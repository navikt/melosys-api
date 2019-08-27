package no.nav.melosys.tjenester.gui.saksflyt;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.unntak.AnmodningUnntakService;
import no.nav.melosys.tjenester.gui.RestTjeneste;
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
    private final BehandlingRepository behandlingRepository;

    @Autowired
    public AnmodningUnntakTjeneste(AnmodningUnntakService anmodningUnntakService, TilgangService tilgangService, BehandlingRepository behandlingRepository) {
        this.anmodningUnntakService = anmodningUnntakService;
        this.tilgangService = tilgangService;
        this.behandlingRepository = behandlingRepository;
    }

    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("{behandlingID}/bestill")
    @ApiOperation(value = "Anmodning om unntak for en gitt behandling")
    public Response anmodningOmUnntak(@PathParam("behandlingID") long behandlingID) throws FunksjonellException, TekniskException {
        tilgangService.sjekkTilgang(behandlingID);
        anmodningUnntakService.anmodningOmUnntak(behandlingID);
        return Response.ok().build();
    }

    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("{behandlingID}/svar")
    @ApiOperation(value = "Sender et svar på anmodning om unntak basert på AnmodningsperiodeSvar som er registrert på behandlingen")
    public Response svar(@PathParam("behandlingID") long behandlingID) throws MelosysException {
        tilgangService.sjekkTilgang(behandlingID);
        validerBehandlingsTypeUnntak(behandlingID);
        anmodningUnntakService.anmodningOmUnntakSvar(behandlingID);
        return Response.ok().build();
    }

    private void validerBehandlingsTypeUnntak(long behandlingId) throws IkkeFunnetException {
        Behandling behandling = behandlingRepository.findById(behandlingId)
            .orElseThrow(() -> new IkkeFunnetException("Finner ikke behandling med id" + behandlingId));

        if (behandling.getType() != Behandlingstyper.ANMODNING_OM_UNNTAK_HOVEDREGEL) {
            throw new BadRequestException("Behandling er ikke av type ANMODNING_OM_UNNTAK_HOVEDREGEL");
        } else if (behandling.getStatus() == Behandlingsstatus.AVSLUTTET) {
            throw new BadRequestException("Behandlingen er avsluttet");
        }
    }
}
