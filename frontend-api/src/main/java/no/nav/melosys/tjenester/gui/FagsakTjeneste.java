package no.nav.melosys.tjenester.gui;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.RolleType;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.service.FagsakService;
import no.nav.melosys.tjenester.gui.dto.BehandlingDto;
import no.nav.melosys.tjenester.gui.dto.FagsakDto;
import no.nav.melosys.tjenester.gui.dto.FagsakOppsummeringDto;
import no.nav.melosys.tjenester.gui.dto.PeriodeDto;
import no.nav.melosys.tjenester.gui.dto.converter.SaksopplysningerTilDtoConverter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static no.nav.melosys.domain.util.SaksopplysningerUtil.hentDokument;
import static no.nav.melosys.domain.util.SoeknadUtil.hentLand;
import static no.nav.melosys.domain.util.SoeknadUtil.hentPeriode;

@Api(tags = {"fagsak"})
@Path("/fagsaker")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
@Transactional
public class FagsakTjeneste extends RestTjeneste {

    private FagsakService fagsakService;

    private ModelMapper modelMapper;

    @Autowired
    public FagsakTjeneste(FagsakService fagsakService) {
        this.fagsakService = fagsakService;

        this.modelMapper = new ModelMapper();

        TypeMap<Behandling, BehandlingDto> typeMapBehandlingUt = modelMapper.createTypeMap(Behandling.class, BehandlingDto.class);
        typeMapBehandlingUt.<Long>addMapping(Behandling::getId, (dest, id) -> dest.getOppsummering().setBehandlingID(id));
        typeMapBehandlingUt.<BehandlingStatus>addMapping(Behandling::getStatus, (dest, status) -> dest.getOppsummering().setStatus(status));
        typeMapBehandlingUt.<BehandlingType>addMapping(Behandling::getType, (dest, type) -> dest.getOppsummering().setType(type));
        typeMapBehandlingUt.<LocalDateTime>addMapping(Behandling::getRegistrertDato, (dest, dato) -> dest.getOppsummering().setRegistrertDato(dato));
        typeMapBehandlingUt.<LocalDateTime>addMapping(Behandling::getEndretDato, (dest, dato) -> dest.getOppsummering().setEndretDato(dato));
        typeMapBehandlingUt.addMappings(mapper -> mapper.using(new SaksopplysningerTilDtoConverter()).map(Behandling::getSaksopplysninger, BehandlingDto::setSaksopplysninger));
    }

    @GET
    @Path("{saksnr}")
    @ApiOperation(value = "Henter en sak med et gitt saksnummer", notes = ("Spesifikke saker kan hentes via saksnummer."))
    public Response hentFagsak(@PathParam("saksnr") @ApiParam("Saksnummer.") String saksnummer) {
        Fagsak sak = fagsakService.hentFagsak(saksnummer);

        if (sak == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            FagsakDto fagsakDto = tilDto(sak);
            return Response.ok(fagsakDto).build();
        }

    }

    @GET
    @Path("ny/{fnr}")
    @ApiOperation(value = "Oppretter en ny sak med et gitt fødselsnummer.")
    public Response nyFagsakSikret(@PathParam("fnr") @ApiParam("Fødselsnummer.") String fnr) {

        // FIXME Midlertidig tilgangskontroll
        Tilgangskontroll.sjekk();

        return nyFagsak(fnr);
    }

    @Deprecated // FIXME Nye saker kommer gjennom journalføring
    public Response nyFagsak(String fnr) {
        try {
            Fagsak fagsak = fagsakService.nyFagsak(fnr);

            if (fagsak == null) {
                return Response.status(Response.Status.NOT_FOUND).build();
            } else {
                FagsakDto fagsakDto = tilDto(fagsak);
                return Response.ok(fagsakDto).build();
            }
        } catch (SikkerhetsbegrensningException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    @GET
    @Path("/sok")
    @ApiOperation(value = "Søk etter saker på fødselsnummer eller d-nummer", notes = ("Saker knyttet til en bruker søkes via fødselsnummer eller d-nummer."))
    public List<FagsakOppsummeringDto> hentFagsaker(@QueryParam("fnr") @ApiParam("Fødselsnummer eller D-nummer.")  String fnr) {
        Iterable<Fagsak> saker = null;

        if (fnr == null) {
            saker = fagsakService.hentAlle();
        } else {
            // TODO Oppslag mot TPS for å få aktørID
            String aktørID = fnr; // test data har aktørID = fnr
            saker = fagsakService.hentFagsaker(RolleType.BRUKER, aktørID);
        }
        return tilDtoer(saker);
    }

    private FagsakDto tilDto(Fagsak fagsak) {
        FagsakDto fagsakDto = new FagsakDto();
        modelMapper.map(fagsak, fagsakDto);
        fagsakDto.setSaksnummer(fagsak.getSaksnummer());
        return fagsakDto;
    }

    private List<FagsakOppsummeringDto> tilDtoer(Iterable<Fagsak> saker) {
        List<FagsakOppsummeringDto> fagsakListe = new ArrayList<>();

        for (Fagsak fagsak : saker) {
            FagsakOppsummeringDto fagsakOppsummeringDto = new FagsakOppsummeringDto();
            fagsakOppsummeringDto.setSaksnummer(fagsak.getSaksnummer());
            fagsakOppsummeringDto.setSakstype(fagsak.getType());
            fagsakOppsummeringDto.setRegistrertDato(fagsak.getRegistrertDato());

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
