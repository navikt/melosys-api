package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.tjenester.gui.dto.BehandlingsresultatDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = { "behandlingsresultat" })
@Path("/behandlingsresultat")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class BehandlingsresultatTjeneste extends RestTjeneste {

    private final Tilgang tilgang;
    private BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public BehandlingsresultatTjeneste(BehandlingsresultatService behandlingsresultatService, Tilgang tilgang) {
        this.behandlingsresultatService = behandlingsresultatService;
        this.tilgang = tilgang;
    }

    @GET
    @Path("{behandlingID}")
    @ApiOperation(value = "Hent behandlingsresultat knyttet til en behandling",
        response = BehandlingsresultatDto.class)
    public Response hentBehandlingsresultat(@PathParam("behandlingID") long behandlingID) throws FunksjonellException, TekniskException {
        tilgang.sjekk(behandlingID);

        Behandlingsresultat resultat = behandlingsresultatService.hentBehandlingsresultat(behandlingID);
        return Response.ok(new BehandlingsresultatDto(resultat)).build();
    }
}
