package no.nav.melosys.tjenester.gui;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.SoeknadService;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.validering.ValideringService;
import no.nav.melosys.tjenester.gui.dto.SoeknadDto;
import no.nav.melosys.tjenester.gui.dto.SoeknadInnDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"soknad"})
@Path("/soknader")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
@Transactional
public class SoeknadTjeneste extends RestTjeneste {

    private final SoeknadService soeknadService;

    private final ValideringService valideringService;

    private final Tilgang tilgang;

    @Autowired
    public SoeknadTjeneste(SoeknadService soeknadService, ValideringService valideringService, Tilgang tilgang) {
        this.soeknadService = soeknadService;
        this.valideringService = valideringService;
        this.tilgang = tilgang;
    }

    @GET
    @Path("{behandlingID}")
    @ApiOperation(value = "Henter en søknad som hører til en gitt behandling", notes = ("Spesifikke saker kan hentes via saksnummer."))
    public Response hentSøknad(@PathParam("behandlingID") long behandlingID) {
        SoeknadDokument soeknadDokument;

        try {
            tilgang.sjekk(behandlingID);
            soeknadDokument = soeknadService.hentSoeknad(behandlingID);
        } catch (IkkeFunnetException e) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } catch (SikkerhetsbegrensningException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (TekniskException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        SoeknadDto soeknadDto;
        if (soeknadDokument == null) {
            soeknadDto = new SoeknadDto(behandlingID, new SoeknadDokument());
        } else {
            soeknadDto = new SoeknadDto(behandlingID, soeknadDokument);
        }

        return Response.ok(soeknadDto).build();
    }

    @POST
    @Path("{behandlingID}")
    @ApiOperation(value = "Tjeneste for å registrere opplysninger fra papirsøknaden manuelt.")
    public Response registrerSøknad(@PathParam("behandlingID") long behandlingID, @ApiParam("Søknadsdata") SoeknadInnDto soeknadInnDto) {
        SoeknadDokument soeknadDokument = soeknadInnDto.getSoknadDokument();

        try {
            tilgang.sjekk(behandlingID);
        } catch (SikkerhetsbegrensningException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (TekniskException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        valideringService.validerOpplysninger(soeknadDokument);
        SoeknadDokument soeknad = soeknadService.registrerSøknad(behandlingID, soeknadDokument);

        SoeknadDto soeknadDto = new SoeknadDto(behandlingID, soeknad);
        return Response.ok(soeknadDto).build();
    }
}
