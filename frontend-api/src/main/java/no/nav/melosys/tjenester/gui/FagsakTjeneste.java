package no.nav.melosys.tjenester.gui;

import java.time.Instant;
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
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.service.sak.FagsakService;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import no.nav.melosys.tjenester.gui.dto.*;
import no.nav.melosys.tjenester.gui.dto.converter.SaksopplysningerTilDtoConverter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static no.nav.melosys.domain.util.SaksopplysningerUtils.hentDokument;
import static no.nav.melosys.domain.util.SoeknadUtils.hentLand;
import static no.nav.melosys.domain.util.SoeknadUtils.hentPeriode;

@Api(tags = {"fagsaker"})
@Path("/fagsaker")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
@Transactional
public class FagsakTjeneste extends RestTjeneste {

    private static final String UKJENT_SAMMENSATT_NAVN = "UKJENT";

    private final FagsakService fagsakService;

    private ModelMapper modelMapper;

    private final Tilgang tilgang;

    @Autowired
    public FagsakTjeneste(FagsakService fagsakService, Tilgang tilgang) {
        this.fagsakService = fagsakService;
        this.tilgang = tilgang;

        this.modelMapper = new ModelMapper();

        TypeMap<Fagsak, FagsakDto> typeMapFagsakUt = modelMapper.createTypeMap(Fagsak.class, FagsakDto.class);
        typeMapFagsakUt.addMapping(Fagsak::getType, FagsakDto::setSakstype);
        typeMapFagsakUt.addMapping(Fagsak::getStatus, FagsakDto::setSaksstatus);
        TypeMap<Behandling, BehandlingDto> typeMapBehandlingUt = modelMapper.createTypeMap(Behandling.class, BehandlingDto.class);
        typeMapBehandlingUt.<Long>addMapping(Behandling::getId, (dest, id) -> dest.getOppsummering().setBehandlingID(id));
        typeMapBehandlingUt.<Behandlingsstatus>addMapping(Behandling::getStatus, (dest, status) -> dest.getOppsummering().setBehandlingsstatus(status));
        typeMapBehandlingUt.<Behandlingstyper>addMapping(Behandling::getType, (dest, type) -> dest.getOppsummering().setBehandlingstype(type));
        typeMapBehandlingUt.<Instant>addMapping(Behandling::getRegistrertDato, (dest, dato) -> dest.getOppsummering().setRegistrertDato(dato));
        typeMapBehandlingUt.<Instant>addMapping(Behandling::getEndretDato, (dest, dato) -> dest.getOppsummering().setEndretDato(dato));
        typeMapBehandlingUt.<Instant>addMapping(Behandling::getSistOpplysningerHentetDato, (dest, dato) -> dest.getOppsummering().setSisteOpplysningerHentetDato(dato));
        typeMapBehandlingUt.addMappings(mapper -> mapper.using(new SaksopplysningerTilDtoConverter()).map(Behandling::getSaksopplysninger, BehandlingDto::setSaksopplysninger));
    }

    @GET
    @Path("{saksnr}")
    @ApiOperation(value = "Henter en sak med et gitt saksnummer", notes = ("Spesifikke saker kan hentes via saksnummer."), response = Fagsak.class)
    public Response hentFagsak(@ApiParam @PathParam("saksnr") String saksnummer) throws FunksjonellException, TekniskException {
        String ident = SubjectHandler.getInstance().getUserID();
        Fagsak sak = fagsakService.hentFagsak(saksnummer);
        tilgang.sjekkSak(sak);
        FagsakDto fagsakDto = tilDto(sak, ident);
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
        tilgang.sjekkFnr(fnr);
        saker = fagsakService.hentFagsakerMedAktør(Aktoersroller.BRUKER, fnr);
        return tilDtoer(saker);
    }

    @POST
    @Path("{saksnr}/henlegg")
    @ApiOperation(value = "Henlegger en fagsak")
    public Response henleggFagsak(@ApiParam @PathParam("saksnr") String saksnummer, @ApiParam("henleggelseDto") HenleggelseDto henleggelseDto) throws FunksjonellException, TekniskException {
        Fagsak sak = fagsakService.hentFagsak(saksnummer);
        tilgang.sjekkSak(sak);

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
        tilgang.sjekkSak(fagsak);

        fagsakService.avsluttSakSomBortfalt(fagsak);
        return Response.noContent().build();
    }

    private FagsakDto tilDto(Fagsak fagsak, String ident) throws FunksjonellException, TekniskException {
        FagsakDto fagsakDto = new FagsakDto();
        modelMapper.map(fagsak, fagsakDto);
        fagsakDto.setSaksnummer(fagsak.getSaksnummer());

        Optional<Behandling> behandling = fagsakService.finnRedigerbarBehandling(ident, fagsak);

        behandling.ifPresent(aktivBehandling -> fagsakDto.getBehandlinger().stream()
                .filter(behandlingDto -> behandlingDto.getOppsummering().getBehandlingID().equals(aktivBehandling.getId()))
                .findAny()
                .ifPresent(behandlingDto -> behandlingDto.setRedigerbart(true))
        );
        //Vi ønsker å ha redigerbare behandlinger først, fordi GUI henter bare ut det første behandlingselementet
        fagsakDto.getBehandlinger().sort(Comparator.comparing(BehandlingDto::isRedigerbart).reversed());

        return fagsakDto;
    }

    private List<FagsakOppsummeringDto> tilDtoer(Iterable<Fagsak> saker) {
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
                    behandlingOversiktDto.setLand(hentLand(soeknadDokument));
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
