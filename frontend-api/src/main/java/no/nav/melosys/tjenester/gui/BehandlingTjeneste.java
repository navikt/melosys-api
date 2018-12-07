package no.nav.melosys.tjenester.gui;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
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
public class BehandlingTjeneste {

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
}
