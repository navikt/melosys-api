package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.service.SoeknadService;
import no.nav.melosys.service.validering.ValideringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@Api(tags = {"soknad"})
@Path("/soknader")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
@Transactional
public class SoknadRestTjeneste extends RestTjeneste  {

    private SoeknadService soeknadService;

    private ValideringService valideringService;

    @Autowired
    public SoknadRestTjeneste(SoeknadService soeknadService, ValideringService valideringService) {
        this.soeknadService = soeknadService;
        this.valideringService = valideringService;
    }

    @GET
    @Path("{behandlingID}")
    @ApiOperation(value = "Henter en søknad som hører til en gitt behandling", notes = ("Spesifikke saker kan hentes via saksnummer."))
    public Response hentSøknad(@PathParam("behandlingID") long behandlingID) {
        SoeknadDokument soeknad = soeknadService.hentSoeknad(behandlingID);

        if (soeknad == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.ok(soeknad).build();
        }
    }

    @POST
    @Path("{behandlingID}")
    @ApiOperation(value = "Tjeneste for å registrere opplysninger fra papirsøknaden manuelt.")
    public Response registrerSøknad(@PathParam("behandlingID") long behandlingID, @ApiParam("Søknadsdata") SoeknadDokument soeknadDokument) {
        valideringService.validerOpplysninger(soeknadDokument);
        SoeknadDokument soeknad = soeknadService.registrerSøknad(behandlingID, soeknadDokument);

        return Response.ok(soeknad).build();
    }

}
