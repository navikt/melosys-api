package no.nav.melosys.tjenester.gui;

import java.util.*;
import java.util.stream.Collectors;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.RegistreringsInfo;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.SoeknadService;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.tjenester.gui.dto.*;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import static no.nav.melosys.domain.util.SoeknadUtils.hentPeriode;
import static no.nav.melosys.domain.util.SoeknadUtils.hentSøknadsland;

@Api(tags = {"fagsaker"})
@Path("/fagsaker")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class FagsakTjeneste extends RestTjeneste {
    private static final String UKJENT_SAMMENSATT_NAVN = "UKJENT";

    private final FagsakService fagsakService;
    private final SaksopplysningerService saksopplysningerService;
    private final SoeknadService søknadService;
    private final TilgangService tilgangService;

    @Autowired
    public FagsakTjeneste(FagsakService fagsakService,
                          SaksopplysningerService saksopplysningerService,
                          SoeknadService soeknadService,
                          TilgangService tilgangService) {
        this.fagsakService = fagsakService;
        this.saksopplysningerService = saksopplysningerService;
        this.søknadService = soeknadService;
        this.tilgangService = tilgangService;
    }

    @GET
    @Path("{saksnr}")
    @ApiOperation(value = "Henter en sak med et gitt saksnummer", notes = ("Spesifikke saker kan hentes via saksnummer."), response = Fagsak.class)
    public Response hentFagsak(@ApiParam @PathParam("saksnr") String saksnummer) throws FunksjonellException, TekniskException {
        Fagsak sak = fagsakService.hentFagsak(saksnummer);
        tilgangService.sjekkSak(sak);
        FagsakDto fagsakDto = tilFagsakDto(sak);
        return Response.ok(fagsakDto).build();
    }

    @GET
    @Path("/sok")
    @ApiOperation(
        value = "Søk etter saker på fødselsnummer eller d-nummer",
        notes = ("Saker knyttet til en bruker søkes via fødselsnummer eller d-nummer."),
        response = FagsakOppsummeringDto.class,
        responseContainer = "List")
    public List<FagsakOppsummeringDto> hentFagsaker(@QueryParam("fnr") @ApiParam("Fødselsnummer eller D-nummer.") String fnr) throws FunksjonellException {
        Iterable<Fagsak> saker;
        if (fnr == null) {
            throw new BadRequestException();
        }
        tilgangService.sjekkFnr(fnr);
        saker = fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, fnr);
        return tilFagsakOppsummeringDtoer(saker);
    }

    @POST
    @Path("{saksnr}/henlegg")
    @ApiOperation(value = "Henlegger en fagsak")
    public Response henleggFagsak(@ApiParam @PathParam("saksnr") String saksnummer, @ApiParam("henleggelseDto") HenleggelseDto henleggelseDto) throws FunksjonellException, TekniskException {
        Fagsak sak = fagsakService.hentFagsak(saksnummer);
        tilgangService.sjekkSak(sak);

        fagsakService.henleggFagsak(saksnummer, henleggelseDto.getBegrunnelseKode(), henleggelseDto.getFritekst());
        return Response.ok().build();
    }

    @POST
    @Path("{saksnr}/henlegg-videresend")
    @ApiOperation(value = "Videresender søknad for en gitt behandling")
    public Response videresend(@PathParam("saksnr") String saksnummer, @ApiParam("videresendDto") VideresendDto videresendDto) throws FunksjonellException, TekniskException {
        Fagsak sak = fagsakService.hentFagsak(saksnummer);
        tilgangService.sjekkSak(sak);

        fagsakService.henleggOgVideresend(saksnummer, videresendDto.getMottakerinstitusjon());
        return Response.ok().build();
    }

    @PUT
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.TEXT_PLAIN)
    @Path("{saksnr}/avsluttsaksombortfalt")
    @ApiOperation(value = "Avslutter en fagsak i Melosys som bortfalt, fordi den ikke skal behandles i Melosys")
    public Response avsluttSakSomBortfalt(@PathParam("saksnr") String saksnummer) throws FunksjonellException, TekniskException {
        Fagsak fagsak = fagsakService.hentFagsak(saksnummer);
        tilgangService.sjekkSak(fagsak);

        fagsakService.avsluttSakSomBortfalt(fagsak);
        return Response.noContent().build();
    }

    private FagsakDto tilFagsakDto(Fagsak fagsak) {
        FagsakDto fagsakDto = new FagsakDto();
        fagsakDto.setSaksnummer(fagsak.getSaksnummer());
        fagsakDto.setGsakSaksnummer(fagsak.getGsakSaksnummer());
        fagsakDto.setSakstype(fagsak.getType());
        fagsakDto.setSaksstatus(fagsak.getStatus());
        fagsakDto.setRegistrertDato(fagsak.getRegistrertDato());
        fagsakDto.setEndretDato(fagsak.getEndretDato());

        return fagsakDto;
    }

    private List<FagsakOppsummeringDto> tilFagsakOppsummeringDtoer(Iterable<Fagsak> saker) {
        List<FagsakOppsummeringDto> fagsakListe = new ArrayList<>();
        for (Fagsak fagsak : saker) {
            FagsakOppsummeringDto fagsakOppsummeringDto = new FagsakOppsummeringDto();
            fagsakOppsummeringDto.setSaksnummer(fagsak.getSaksnummer());
            fagsakOppsummeringDto.setSakstype(fagsak.getType());
            fagsakOppsummeringDto.setSaksstatus(fagsak.getStatus());
            fagsakOppsummeringDto.setOpprettetDato(fagsak.getRegistrertDato());

            List<Behandling> behandlinger = fagsak.getBehandlinger();

            List<BehandlingOversiktDto> behandlingOversiktDtoer = behandlinger.stream()
                .sorted(Comparator.comparing(RegistreringsInfo::getRegistrertDato).reversed())
                .map(this::tilBehandlingOversiktDto)
                .collect(Collectors.toList());

            fagsakOppsummeringDto.setSammensattNavn(hentSammensattNavn(behandlinger));
            fagsakOppsummeringDto.setBehandlingOversikter(behandlingOversiktDtoer);
            fagsakListe.add(fagsakOppsummeringDto);
        }
        return fagsakListe;
    }

    private BehandlingOversiktDto tilBehandlingOversiktDto(Behandling behandling) {
        BehandlingOversiktDto behandlingOversiktDto = new BehandlingOversiktDto();
        if (behandling != null) {
            behandlingOversiktDto.setBehandlingID(behandling.getId());
            behandlingOversiktDto.setBehandlingsstatus(behandling.getStatus());
            behandlingOversiktDto.setBehandlingstype(behandling.getType());
            behandlingOversiktDto.setOpprettetDato(behandling.getRegistrertDato());

            setPeriodeOpplysninger(behandling, behandlingOversiktDto);
        }
        return behandlingOversiktDto;
    }

    private void setPeriodeOpplysninger(Behandling behandling, BehandlingOversiktDto behandlingOversiktDto) {
        if (behandling.getType() == Behandlingstyper.SOEKNAD) {
            søknadService.finnSøknad(behandling.getId()).ifPresent(soeknadDokument -> {
                    behandlingOversiktDto.setLand(hentSøknadsland(soeknadDokument));
                    Periode periode = hentPeriode(soeknadDokument);
                    behandlingOversiktDto.setPeriode(new PeriodeDto(periode.getFom(), periode.getTom()));
                });
        } else {
            saksopplysningerService.finnSedOpplysninger(behandling.getId()).ifPresent(sedDokument -> {
                behandlingOversiktDto.setLand(Collections.singletonList(sedDokument.getLovvalgslandKode() != null
                    ? sedDokument.getLovvalgslandKode().getKode() : null));
                behandlingOversiktDto.setPeriode(new PeriodeDto(
                    sedDokument.getLovvalgsperiode().getFom(), sedDokument.getLovvalgsperiode().getTom())
                );
            });
        }
    }

    private String hentSammensattNavn(List<Behandling> behandlinger) {
        if (behandlinger.isEmpty()) {
            return UKJENT_SAMMENSATT_NAVN;
        }

        Optional<PersonDokument> saksopplysningPerson = saksopplysningerService.finnPersonOpplysninger(behandlinger.get(0).getId());
        if (saksopplysningPerson.isPresent()) {
            return saksopplysningPerson.get().sammensattNavn;
        } else {
            return UKJENT_SAMMENSATT_NAVN;
        }
    }
}
