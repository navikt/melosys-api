package no.nav.melosys.tjenester.gui;

import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.*;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.SoeknadService;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.tjenester.gui.dto.SoeknadDto;
import no.nav.melosys.tjenester.gui.dto.SoeknadTilleggsDataDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"søknad"})
@Path("/soknader")
@Service
@Scope(value = WebApplicationContext.SCOPE_REQUEST)
public class SoeknadTjeneste extends RestTjeneste {

    private final SoeknadService soeknadService;

    private final RegisterOppslagService registerOppslagService;

    private final TilgangService tilgangService;

    @Autowired
    public SoeknadTjeneste(SoeknadService soeknadService, RegisterOppslagService registerOppslagService, TilgangService tilgangService) {
        this.soeknadService = soeknadService;
        this.registerOppslagService = registerOppslagService;
        this.tilgangService = tilgangService;
    }

    @GET
    @Path("{behandlingID}")
    @ApiOperation(
        value = "Henter en søknad som hører til en gitt behandling",
        notes = ("Spesifikke saker kan hentes via saksnummer."),
        response = SoeknadDto.class)
    public Response hentSøknad(@ApiParam @PathParam("behandlingID") long behandlingID) throws TekniskException, IkkeFunnetException, SikkerhetsbegrensningException {
        SoeknadDokument soeknadDokument;
        tilgangService.sjekk(behandlingID);
        soeknadDokument = soeknadService.hentSoeknad(behandlingID);
        SoeknadTilleggsDataDto tilleggDataDto = hentTilleggsData(soeknadDokument);
        SoeknadDto soeknadDto;
        soeknadDto = new SoeknadDto(behandlingID, soeknadDokument, tilleggDataDto);
        return Response.ok(soeknadDto).build();
    }

    @POST
    @Path("{behandlingID}")
    @ApiOperation(
        value = "Tjeneste for å registrere opplysninger fra papirsøknaden manuelt.",
        response = SoeknadDto.class)
    public SoeknadDto registrerSøknad(@ApiParam SoeknadDto soeknadInnDto) throws FunksjonellException, TekniskException {
        long behandlingID = soeknadInnDto.getBehandlingID();
        SoeknadDokument soeknadDokument = soeknadInnDto.getSoeknadDokument();
        tilgangService.sjekkRedigerbar(behandlingID);
        soeknadDokument = soeknadService.registrerSøknad(behandlingID, soeknadDokument);
        SoeknadTilleggsDataDto tilleggDataDto = hentTilleggsData(soeknadDokument);
        return new SoeknadDto(behandlingID, soeknadDokument, tilleggDataDto);
    }

    public SoeknadTilleggsDataDto hentTilleggsData(SoeknadDokument soeknadDokument) throws IkkeFunnetException, SikkerhetsbegrensningException, IntegrasjonException {
        Set<OrganisasjonDokument> organisasjoner;
        Set<PersonDokument> personer;

        Set<String> organisasjonsnummer = soeknadDokument.hentAlleOrganisasjonsnumre();
        organisasjoner = registerOppslagService.hentOrganisasjoner(organisasjonsnummer);
        Set<String> personnumre = soeknadDokument.hentAllePersonnumre();
        personer = registerOppslagService.hentPersoner(personnumre);

        return new SoeknadTilleggsDataDto(organisasjoner, personer);
    }
}
