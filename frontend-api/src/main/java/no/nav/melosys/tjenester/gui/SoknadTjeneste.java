package no.nav.melosys.tjenester.gui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.service.SoeknadService;
import no.nav.melosys.service.validering.ValideringService;
import no.nav.melosys.tjenester.gui.dto.SoeknadDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

@Api(tags = {"soknad"})
@Path("/soknader")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
@Transactional
public class SoknadTjeneste extends RestTjeneste  {

    private SoeknadService soeknadService;

    private ValideringService valideringService;

    @Autowired
    public SoknadTjeneste(SoeknadService soeknadService, ValideringService valideringService) {
        this.soeknadService = soeknadService;
        this.valideringService = valideringService;
    }

    @GET
    @Path("{behandlingID}")
    @ApiOperation(value = "Henter en søknad som hører til en gitt behandling", notes = ("Spesifikke saker kan hentes via saksnummer."))
    public Response hentSøknad(@PathParam("behandlingID") long behandlingID) {
        SoeknadDokument soeknad = null;

        try {
            soeknad = soeknadService.hentSoeknad(behandlingID);
        } catch (NotFoundException notFoundException) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        SoeknadDto soeknadDto = new SoeknadDto(behandlingID, soeknad);
        return Response.ok(soeknadDto).build();
    }

    @POST
    @Path("{behandlingID}")
    @ApiOperation(value = "Tjeneste for å registrere opplysninger fra papirsøknaden manuelt.")
    public Response registrerSøknad(@PathParam("behandlingID") long behandlingID, @ApiParam("Søknadsdata") SoeknadDokument soeknadDokument) {
        valideringService.validerOpplysninger(soeknadDokument);
        SoeknadDokument soeknad = soeknadService.registrerSøknad(behandlingID, soeknadDokument);

        SoeknadDto soeknadDto = new SoeknadDto(behandlingID, soeknad);
        return Response.ok(soeknadDto).build();
    }

}
