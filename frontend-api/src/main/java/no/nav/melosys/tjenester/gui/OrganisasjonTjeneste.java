package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.integrasjon.felles.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.RegisterOppslagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"organisasjon"})
@Path("/organisasjoner")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class OrganisasjonTjeneste extends RestTjeneste {

    private RegisterOppslagService registerOppslag;

    @Autowired
    public OrganisasjonTjeneste(RegisterOppslagService registerOppslag) {
        this.registerOppslag = registerOppslag;
    }

    @GET
    @ApiOperation(value = "Henter en organisasjon fra Enhetsregisteret.")
    public Response hentOrganisasjon(@QueryParam("orgnr") String orgnummer) {
        if (orgnummer == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        OrganisasjonDokument organisasjonDokument;
        try {
            organisasjonDokument = registerOppslag.hentOrganisasjon(orgnummer);
        } catch (SikkerhetsbegrensningException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (IkkeFunnetException e) {
            return Response.ok(RestTjeneste.TOM_JSON).build();
        }

        return Response.ok(organisasjonDokument).build();
    }
}
