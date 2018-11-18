package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.tjenester.gui.dto.PersonDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"person"})
@Path("/personer")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class PersonTjeneste extends RestTjeneste {
    
    private static final Logger log = LoggerFactory.getLogger(PersonTjeneste.class);

    private RegisterOppslagService registerOppslag;

    private final Tilgang tilgang;

    @Autowired
    public PersonTjeneste(RegisterOppslagService registerOppslag, Tilgang tilgang) {
        this.registerOppslag = registerOppslag;
        this.tilgang = tilgang;
    }

    @GET
    @ApiOperation(value = "Henter en person fra TPS.", response = PersonDokument.class)
    public Response getPerson(@QueryParam("fnr") @ApiParam("Fødselsnummer eller D-nummer.") String personnummer) throws SikkerhetsbegrensningException, IkkeFunnetException, IntegrasjonException {
        if (personnummer == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        PersonDokument personDokument;
        personDokument = registerOppslag.hentPerson(personnummer);
        tilgang.sjekkFnr(personnummer);
        return Response.ok(new PersonDto(personDokument)).build();
    }
}
