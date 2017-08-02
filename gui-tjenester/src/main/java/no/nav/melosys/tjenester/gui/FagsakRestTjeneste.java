package no.nav.melosys.tjenester.gui;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
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
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.repository.FagsakRepository;

@Api(tags = {"fagsak"})
@Path("/fagsaker")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class FagsakRestTjeneste {

    private FagsakRepository fagsakRepository;

    @Autowired
    public FagsakRestTjeneste(FagsakRepository fagsakRepository) {
        this.fagsakRepository = fagsakRepository;
    }

    @GET
    @Path("{saksnummer}")
    @ApiOperation(value = "Henter en sak med et gitt saksnummer", notes = ("Spesifikke saker kan hentes via saksnummer."))
    public  Fagsak hentFagsak(@PathParam("saksnummer") @ApiParam("Saksnummer.") Long saksnummer) {
        return fagsakRepository.findBySaksnummer(saksnummer);
    }

    @GET
    @ApiOperation(value = "Søk etter saker på fødselsnummer eller d-nummer", notes = ("Saker knyttet til en bruker søkes via fødselsnummer eller d-nummer."))
    public List<Fagsak> hentFagsaker(
            @QueryParam("fnr") @ApiParam("Fødselsnummer eller D-nummer.") String fnr) {
        List<Fagsak> saker = fagsakRepository.findByFnr(fnr);
        return saker;
    }

}
