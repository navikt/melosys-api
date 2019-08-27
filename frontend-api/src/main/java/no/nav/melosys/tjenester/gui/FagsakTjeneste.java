package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.TilgangService;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.tjenester.gui.dto.*;
import no.nav.melosys.tjenester.gui.dto.periode.PeriodeDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static no.nav.melosys.domain.util.SaksopplysningerUtils.hentDokument;
import static no.nav.melosys.domain.util.SoeknadUtils.hentPeriode;
import static no.nav.melosys.domain.util.SoeknadUtils.hentSøknadsland;

@Api(tags = {"fagsaker"})
@Path("/fagsaker")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
@Transactional
public class FagsakTjeneste extends RestTjeneste {

    private static final String UKJENT_SAMMENSATT_NAVN = "UKJENT";

    private final FagsakService fagsakService;

    private final TilgangService tilgangService;

    @Autowired
    public FagsakTjeneste(FagsakService fagsakService, TilgangService tilgangService) {
        this.fagsakService = fagsakService;
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

            setSammensattNavn(fagsakOppsummeringDto, behandlinger.get(0));
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

            hentDokument(behandling, SaksopplysningType.SØKNAD).ifPresent(
                saksopplysningDokument -> {
                    SoeknadDokument soeknadDokument = (SoeknadDokument) saksopplysningDokument;
                    behandlingOversiktDto.setLand(hentSøknadsland(soeknadDokument));
                    Periode periode = hentPeriode(soeknadDokument);
                    behandlingOversiktDto.setSoknadsperiode(new PeriodeDto(periode.getFom(), periode.getTom()));
                });
            }
        return behandlingOversiktDto;
    }

    private void setSammensattNavn(FagsakOppsummeringDto fagsakOppsummeringDto, Behandling behandling) {
        Optional<SaksopplysningDokument> saksopplysningDokumentPerson = hentDokument(behandling, SaksopplysningType.PERSOPL);

        if( saksopplysningDokumentPerson.isPresent()) {
                PersonDokument personDokument = (PersonDokument) saksopplysningDokumentPerson.get();
                fagsakOppsummeringDto.setSammensattNavn(personDokument.sammensattNavn);
        } else {
            fagsakOppsummeringDto.setSammensattNavn(UKJENT_SAMMENSATT_NAVN);
        }
    }
}
