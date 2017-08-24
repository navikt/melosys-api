package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import io.swagger.annotations.Api;
import no.nav.melosys.repository.ArbeidsforholdRepository;

@Api(tags = {"arbeidsforhold"})
@Path("/arbeidsforhold")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class ArbeidsforholdRestTjeneste extends RestTjeneste {

    private ArbeidsforholdRepository repository;

    @Autowired
    public ArbeidsforholdRestTjeneste(ArbeidsforholdRepository repository) {
        this.repository = repository;
    }

    /* FIXME
    @GET
    @Path("{id}")
    public Arbeidsforhold hentArbeidsforhold(@PathParam("id") Long id) {
        return repository.findOne(id);
    }

    @GET
    public Iterable<Arbeidsforhold> hentArbeidsforhold() {
        return repository.findAll();
    }
    // */


}
