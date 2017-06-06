package no.nav.melosys.controller;

import no.nav.melosys.domain.Bruker;
import no.nav.melosys.service.EksempelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import java.util.Collection;
import java.util.List;

@Component
@Path("/jaxrs")
@Produces("application/json")
public class JaxRsEndpoint {

    @Autowired
    EksempelService eksempelService;

    @Path("/{navn}")
    @GET
    public List<Bruker> findByNavn(@PathParam("navn") String navn) {
        return eksempelService.findByNavn(navn);
    }

    @GET
    public Collection<Bruker> findAll() {
        return eksempelService.findAll();
    }

    @POST
    public Bruker createPerson(Bruker person) {
        return eksempelService.addBruker(person);
    }

    @Path("/{id}")
    @POST
    public Bruker updatePerson(@PathParam("id") Long id, Bruker updatedPerson) {
        return eksempelService.updateBruker(id, updatedPerson);
    }

    @Path("/{id}")
    @DELETE
    public void deletePerson(@PathParam("id") Long id) {
        eksempelService.deletePerson(id);
    }


}