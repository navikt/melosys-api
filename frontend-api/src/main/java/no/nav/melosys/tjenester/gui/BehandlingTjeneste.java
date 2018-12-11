package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.TidligereMedlemsperioderDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = { "behandlinger" })
@Path("/behandlinger")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class BehandlingTjeneste extends RestTjeneste {

    private static final Logger log = LoggerFactory.getLogger(DokumentTjeneste.class);

    private final BehandlingService behandlingService;

    private final Tilgang tilgang;

    @Autowired
    public BehandlingTjeneste(BehandlingService behandlingService, Tilgang tilgang) {
        this.behandlingService = behandlingService;
        this.tilgang = tilgang;
    }

    @POST
    @Path("{behandlingID}/status/{statusKode}")
    @ApiOperation("Oppdaterer status for en behandling. " +
        "Brukes til å markere om saksbehandler fortsatt venter på dokumentasjon eller om behandling kan gjenopptas.")
    public void oppdaterStatus(@PathParam("behandlingID") long behandlingID,
                               @PathParam("statusKode") Behandlingsstatus status) throws FunksjonellException, TekniskException {
        log.info("Saksbehandler {} ber om å endre status for behandling {} til {}.", SubjectHandler.getInstance().getUserID(), behandlingID, status.getKode());
        tilgang.sjekk(behandlingID);
        behandlingService.oppdaterStatus(behandlingID, status);
    }

    @POST
    @Path("{behandlingID}/perioder")
    @ApiOperation(value = "Knytt medlemsperioder til oppholdsland fra søknaden",
        response = TidligereMedlemsperioderDto.class)
    public Response knyttMedlemsperioder(@PathParam("behandlingID") long behandlingID,
                                         TidligereMedlemsperioderDto tidligereMedlemsperioder) throws FunksjonellException, TekniskException {
        log.info("Saksbehandler {} ber om å knytte medlemsperioder for behandling {}.", SubjectHandler.getInstance().getUserID(), behandlingID);
        tilgang.sjekk(behandlingID);

        behandlingService.knyttMedlemsperioder(behandlingID, tidligereMedlemsperioder.periodeIder);
        return Response.ok(tidligereMedlemsperioder).build();
    }

    @GET
    @Path("{behandlingID}/perioder")
    @ApiOperation(value = "Hent medlemsperioder knyttet til oppholdsland fra søknaden",
        response = TidligereMedlemsperioderDto.class)
    public Response hentMedlemsperioder(@PathParam("behandlingID") long behandlingID) throws FunksjonellException, TekniskException {
        log.info("Saksbehandler {} ber om å hente medlemsperioder for behandling {}.", SubjectHandler.getInstance().getUserID(), behandlingID);
        tilgang.sjekk(behandlingID);

        TidligereMedlemsperioderDto tidligereMedlemsperioderDto = new TidligereMedlemsperioderDto();
        tidligereMedlemsperioderDto.periodeIder = behandlingService.hentMedlemsperioder(behandlingID);
        return Response.ok(tidligereMedlemsperioderDto).build();
    }
}
