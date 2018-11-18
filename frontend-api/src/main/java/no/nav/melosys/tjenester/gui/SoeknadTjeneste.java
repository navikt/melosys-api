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
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.IntegrasjonException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.SoeknadService;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.validering.ValideringService;
import no.nav.melosys.tjenester.gui.dto.SoeknadDto;
import no.nav.melosys.tjenester.gui.dto.SoeknadTilleggsDataDto;

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

    private final RegisterOppslagService registerOppslagService;

    private final Tilgang tilgang;

    @Autowired
    public SoeknadTjeneste(SoeknadService soeknadService, ValideringService valideringService, RegisterOppslagService registerOppslagService, Tilgang tilgang) {
        this.soeknadService = soeknadService;
        this.valideringService = valideringService;
        this.registerOppslagService = registerOppslagService;
        this.tilgang = tilgang;
    }

    @GET
    @Path("{behandlingID}")
    @ApiOperation(
        value = "Henter en søknad som hører til en gitt behandling",
        notes = ("Spesifikke saker kan hentes via saksnummer."),
        response = SoeknadDto.class)
    public Response hentSøknad(@ApiParam @PathParam("behandlingID") long behandlingID) throws TekniskException, IkkeFunnetException, SikkerhetsbegrensningException {
        SoeknadDokument soeknadDokument;
        tilgang.sjekk(behandlingID);
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
    public SoeknadDto registrerSøknad(@ApiParam SoeknadDto soeknadInnDto) throws SikkerhetsbegrensningException, IkkeFunnetException, TekniskException {
        long behandlingID = soeknadInnDto.getBehandlingID();
        SoeknadDokument soeknadDokument = soeknadInnDto.getSoeknadDokument();
        tilgang.sjekk(behandlingID);
        valideringService.validerOpplysninger(soeknadDokument);
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
