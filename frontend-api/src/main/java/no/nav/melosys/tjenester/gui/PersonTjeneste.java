package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.tjenester.gui.dto.PersonDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"personer"})
@Path("/personer")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class PersonTjeneste extends RestTjeneste {
    private final RegisterOppslagService registerOppslag;
    private final TilgangService tilgangService;

    @Autowired
    public PersonTjeneste(RegisterOppslagService registerOppslag, TilgangService tilgangService) {
        this.registerOppslag = registerOppslag;
        this.tilgangService = tilgangService;
    }

    @GET
    @Path("{fnr}")
    @ApiOperation(value = "Henter en person fra TPS.", response = PersonDokument.class)
    public Response getPerson(@PathParam("fnr") @ApiParam("Fødselsnummer eller D-nummer.") String personnummer)
        throws SikkerhetsbegrensningException, IkkeFunnetException, IntegrasjonException {
        if (personnummer == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        PersonDokument personDokument = registerOppslag.hentPerson(personnummer);
        tilgangService.sjekkFnr(personnummer);
        return Response.ok(new PersonDto(personDokument)).build();
    }
}
