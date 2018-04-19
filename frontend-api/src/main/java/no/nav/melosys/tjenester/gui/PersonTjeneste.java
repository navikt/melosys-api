package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.RegisterOppslagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"person"})
@Path("/personer")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class PersonTjeneste extends RestTjeneste {

    private RegisterOppslagService registerOppslag;

    @Autowired
    public PersonTjeneste(RegisterOppslagService registerOppslag) {
        this.registerOppslag = registerOppslag;
    }

    @GET
    @ApiOperation(value = "Henter en person fra TPS.")
    public Response getPerson(@QueryParam("fnr") String personnummer) {
        if (personnummer == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        PersonDokument personDokument;
        try {
            personDokument = registerOppslag.hentPerson(personnummer);
        } catch (SikkerhetsbegrensningException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        return Response.ok(personDokument).build();
    }
}
