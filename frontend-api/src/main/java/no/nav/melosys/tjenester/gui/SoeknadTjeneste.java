package no.nav.melosys.tjenester.gui;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.SaksopplysningKilde;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.SoeknadService;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.validering.ValideringService;
import no.nav.melosys.tjenester.gui.dto.SoeknadDto;
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
    @ApiOperation(
        value = "Henter en søknad som hører til en gitt behandling",
        notes = ("Spesifikke saker kan hentes via saksnummer."),
        response = SoeknadDto.class)
    public Response hentSøknad(@ApiParam @PathParam("behandlingID") long behandlingID) {
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
        soeknadDto = new SoeknadDto(behandlingID, soeknadDokument);
        return Response.ok(soeknadDto).build();
    }

    @POST
    @Path("{behandlingID}")
    @ApiOperation(
        value = "Tjeneste for å registrere opplysninger fra papirsøknaden manuelt.",
        response = SoeknadDto.class)
    public SoeknadDto registrerSøknad(@ApiParam SoeknadDto soeknadInnDto) {
        long behandlingID = soeknadInnDto.getBehandlingID();
        SoeknadDokument soeknadDokument = soeknadInnDto.getSoeknadDokument();

        try {
            tilgang.sjekk(behandlingID);
        } catch (SikkerhetsbegrensningException e) {
            throw new ForbiddenException(e);
        } catch (TekniskException e) {
            throw new InternalServerErrorException(e);
        }

        valideringService.validerOpplysninger(soeknadDokument);

        try {
            soeknadDokument = soeknadService.registrerSøknad(behandlingID, soeknadDokument);
        } catch (IkkeFunnetException e) {
            throw new NotFoundException(e.getMessage());
        }

        return new SoeknadDto(behandlingID, soeknadDokument);
    }
}
