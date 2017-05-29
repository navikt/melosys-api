package no.nav.melosys.controller;

import no.nav.melosys.domain.Person;
import no.nav.melosys.service.EksempelService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
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
    public List<Person> findByNavn(@PathParam("navn") String navn) {
        return eksempelService.findByNavn(navn);
    }

    @GET
    public Collection<Person> findAll() {
        return eksempelService.findAll();
    }

    @POST
    public Person createPerson(Person person) {
        return eksempelService.addPerson(person);
    }

    @Path("/{id}")
    @POST
    public Person updatePerson(@PathParam("id") Long id, Person updatedPerson) {
        return eksempelService.updatePerson(id, updatedPerson);
    }


    @Path("/{id}")
    @DELETE
    public void deletePerson(@PathParam("id") Long id) {
        eksempelService.deletePerson(id);
    }


}