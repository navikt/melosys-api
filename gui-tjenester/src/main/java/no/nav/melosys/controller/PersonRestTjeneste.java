package no.nav.melosys.controller;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import no.nav.melosys.domain.Bruker;
import no.nav.melosys.repository.BrukerRepository;

@Api(tags = { "person" })
@Path("/personer")
@Produces(MediaType.APPLICATION_JSON)
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class PersonRestTjeneste {

    private BrukerRepository brukerRepository;

    @Autowired
    public PersonRestTjeneste(BrukerRepository brukerRepository) {
        this.brukerRepository = brukerRepository;
    }

    @GET
    @ApiOperation(value = "Søk en person på fødselsnummer")
    public Bruker findByFnr(@QueryParam("fnr") String fnr) {
        return brukerRepository.findByFnr(fnr);
    }

    @GET
    @Path("/all")
    public Iterable<Bruker> findAll() {
        return brukerRepository.findAll();
    }

    @POST
    public Bruker createPerson(Bruker person) {
        return brukerRepository.save(person);
    }

    @Path("/{id}")
    @DELETE
    public void deletePerson(@PathParam("id") Long id) {
        brukerRepository.delete(id);
    }
}
