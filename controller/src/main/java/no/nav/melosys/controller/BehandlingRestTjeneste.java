package no.nav.melosys.controller;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.repository.BehandlingRepository;

@Api(tags = { "behandling" })
@Path("/behandling")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class BehandlingRestTjeneste {

    private BehandlingRepository behandlingrepo;

    @Autowired
    public BehandlingRestTjeneste(BehandlingRepository behandlingrepo) {
        this.behandlingrepo = behandlingrepo;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Søk etter behandlinger på saksnummer", notes = ("Returnerer alle behandlinger som er tilknyttet saksnummer."))
    public List<Behandling> hentBehandlinger(@QueryParam("snr") @ApiParam("Saksnummer må være et eksisterende saksnummer") Long saksnummer) {
        List<Behandling> behandlinger = behandlingrepo.findBySaksnummer(saksnummer);

        return behandlinger;
    }

}
