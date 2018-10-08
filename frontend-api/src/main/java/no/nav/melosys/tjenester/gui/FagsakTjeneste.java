package no.nav.melosys.tjenester.gui;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.*;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.FagsakService;
import no.nav.melosys.service.abac.Tilgang;
import no.nav.melosys.tjenester.gui.dto.BehandlingDto;
import no.nav.melosys.tjenester.gui.dto.FagsakDto;
import no.nav.melosys.tjenester.gui.dto.FagsakOppsummeringDto;
import no.nav.melosys.tjenester.gui.dto.PeriodeDto;
import no.nav.melosys.tjenester.gui.dto.converter.SaksopplysningerTilDtoConverter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static no.nav.melosys.domain.util.SaksopplysningerUtils.hentDokument;
import static no.nav.melosys.domain.util.SoeknadUtils.hentLand;
import static no.nav.melosys.domain.util.SoeknadUtils.hentPeriode;

@Api(tags = {"fagsak"})
@Path("/fagsaker")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
@Transactional
public class FagsakTjeneste extends RestTjeneste {
    
    private static final Logger log = LoggerFactory.getLogger(FagsakTjeneste.class);

    private FagsakService fagsakService;

    private ModelMapper modelMapper;

    private final Tilgang tilgang;

    @Autowired
    public FagsakTjeneste(FagsakService fagsakService, Tilgang tilgang) {
        this.fagsakService = fagsakService;
        this.tilgang = tilgang;

        this.modelMapper = new ModelMapper();

        TypeMap<Behandling, BehandlingDto> typeMapBehandlingUt = modelMapper.createTypeMap(Behandling.class, BehandlingDto.class);
        typeMapBehandlingUt.<Long>addMapping(Behandling::getId, (dest, id) -> dest.getOppsummering().setBehandlingID(id));
        typeMapBehandlingUt.<Behandlingsstatus>addMapping(Behandling::getStatus, (dest, status) -> dest.getOppsummering().setStatus(status));
        typeMapBehandlingUt.<Behandlingstype>addMapping(Behandling::getType, (dest, type) -> dest.getOppsummering().setType(type));
        typeMapBehandlingUt.<Instant>addMapping(Behandling::getRegistrertDato, (dest, dato) -> dest.getOppsummering().setRegistrertDato(dato));
        typeMapBehandlingUt.<Instant>addMapping(Behandling::getEndretDato, (dest, dato) -> dest.getOppsummering().setEndretDato(dato));
        typeMapBehandlingUt.addMappings(mapper -> mapper.using(new SaksopplysningerTilDtoConverter()).map(Behandling::getSaksopplysninger, BehandlingDto::setSaksopplysninger));
    }

    @GET
    @Path("{saksnr}")
    @ApiOperation(value = "Henter en sak med et gitt saksnummer", notes = ("Spesifikke saker kan hentes via saksnummer."), response = Fagsak.class)
    public Response hentFagsak(@PathParam("saksnr") @ApiParam("Saksnummer.") String saksnummer) {
        Fagsak sak = fagsakService.hentFagsak(saksnummer);
        if (sak == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        try {
            tilgang.sjekk(sak);
        } catch (SikkerhetsbegrensningException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (TekniskException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        FagsakDto fagsakDto = tilDto(sak);
        return Response.ok(fagsakDto).build();
    }

    @Deprecated // FIXME Trengs av test så langt
    @GET
    @Path("ny/{fnr}")
    @ApiOperation(value = "Oppretter en ny sak med et gitt fødselsnummer.", response = Fagsak.class)
    public Response nyFagsakSikret(@PathParam("fnr") @ApiParam("Fødselsnummer.") String fnr) {
        try {
            Fagsak fagsak = fagsakService.testFagsakOgBehandling(fnr, Behandlingstype.SØKNAD);
            tilgang.sjekk(fagsak);
            if (fagsak == null) {
                return Response.status(Response.Status.BAD_REQUEST).build();
            } else {
                return Response.ok(fagsak.getSaksnummer()).build();
            }
        } catch (IkkeFunnetException e) {
            return Response.status(Response.Status.NOT_FOUND).entity("Ident " + fnr + " ikke funnet").build();
        } catch (SikkerhetsbegrensningException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        } catch (TekniskException e) {
            log.error("TekniskException", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/sok")
    @ApiOperation(
        value = "Søk etter saker på fødselsnummer eller d-nummer",
        notes = ("Saker knyttet til en bruker søkes via fødselsnummer eller d-nummer."),
        response = Fagsak.class,
        responseContainer = "List")
    public List<FagsakOppsummeringDto> hentFagsaker(@QueryParam("fnr") @ApiParam("Fødselsnummer eller D-nummer.")  String fnr) {
        Iterable<Fagsak> saker;

        if (fnr == null) {
            throw new BadRequestException();
        } else {
            try {
                tilgang.sjekkFnr(fnr);
                saker = fagsakService.hentFagsakerMedAktør(RolleType.BRUKER, fnr);
            } catch (IkkeFunnetException e) {
                throw new NotFoundException(e.getMessage());
            } catch (SikkerhetsbegrensningException e) {
                throw new ForbiddenException("Ikke tilgang");
            }
        }
        try {
            return tilDtoer(saker);
        } catch (TekniskException e) {
            log.error("TekniskException", e);
            throw new InternalServerErrorException("Intern feil");
        }
    }

    private FagsakDto tilDto(Fagsak fagsak) {
        FagsakDto fagsakDto = new FagsakDto();
        modelMapper.map(fagsak, fagsakDto);
        fagsakDto.setSaksnummer(fagsak.getSaksnummer());
        return fagsakDto;
    }

    private List<FagsakOppsummeringDto> tilDtoer(Iterable<Fagsak> saker) throws TekniskException {
        List<FagsakOppsummeringDto> fagsakListe = new ArrayList<>();

        for (Fagsak fagsak : saker) {
            FagsakOppsummeringDto fagsakOppsummeringDto = new FagsakOppsummeringDto();
            fagsakOppsummeringDto.setSaksnummer(fagsak.getSaksnummer());
            fagsakOppsummeringDto.setSakstype(fagsak.getType());
            fagsakOppsummeringDto.setOpprettetDato(fagsak.getRegistrertDato());

            Behandling behandling = fagsak.getAktivBehandling();
            if (behandling != null) {
                fagsakOppsummeringDto.setBehandlingsstatus(behandling.getStatus());
                fagsakOppsummeringDto.setBehandlingstype(behandling.getType());

                Optional<SaksopplysningDokument> opt = hentDokument(behandling, SaksopplysningType.SØKNAD);
                if (opt.isPresent()) {
                    SoeknadDokument soeknadDokument = (SoeknadDokument) opt.get();
                    fagsakOppsummeringDto.setLand(hentLand(soeknadDokument));

                    Periode periode = hentPeriode(soeknadDokument);
                    fagsakOppsummeringDto.setSoknadsperiode(new PeriodeDto(periode.getFom(), periode.getTom()));
                }
            }

            fagsakListe.add(fagsakOppsummeringDto);
        }

        return fagsakListe;
    }
}
