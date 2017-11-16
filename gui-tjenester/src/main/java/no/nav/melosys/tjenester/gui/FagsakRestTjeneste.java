package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.modelmapper.Converter;
import org.modelmapper.ModelMapper;
import org.modelmapper.TypeMap;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.RolleType;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.domain.dokument.person.PersonopplysningDokument;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.tjenester.gui.dto.BehandlingDto;
import no.nav.melosys.tjenester.gui.dto.FagsakDto;
import no.nav.melosys.tjenester.gui.dto.FagsakOppsummeringDto;

@Api(tags = {"fagsak"})
@Path("/fagsaker")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class FagsakRestTjeneste extends RestTjeneste {

    private FagsakRepository fagsakRepository;

    private DokumentFactory dokumentFactory;

    private ModelMapper modelMapper;

    @Autowired
    public FagsakRestTjeneste(FagsakRepository fagsakRepository, DokumentFactory dokumentFactory) {
        this.fagsakRepository = fagsakRepository;
        this.dokumentFactory = dokumentFactory;

        this.modelMapper = new ModelMapper();
        modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.LOOSE);
        TypeMap<Behandling, BehandlingDto> typeMapBehandling = modelMapper.createTypeMap(Behandling.class, BehandlingDto.class);
        
        Converter<Set, Set> converterSaksopplysning = ctx -> {
            Set dokumenter = new HashSet();
            if (ctx.getSource() != null) {
                ctx.getSource().forEach(x -> dokumenter.add(dokumentFactory.lagDokument((Saksopplysning) x)));
            }
            return dokumenter;
        };

        typeMapBehandling.addMappings(mapper -> mapper.using(converterSaksopplysning).map(Behandling::getSaksopplysninger, BehandlingDto::setSaksopplysninger));
    }

    @GET
    @Path("/fnr/{fnr}")
    @ApiOperation(value = "Søk etter saker på fødselsnummer eller d-nummer", notes = ("Saker knyttet til en bruker søkes via fødselsnummer eller d-nummer."))
    public List<FagsakOppsummeringDto> hentFagsaker(@PathParam("fnr") @ApiParam("Fødselsnummer eller D-nummer.")  String fnr) {
        // TODO Oppslag mot TPS for å få aktørID
        Map<String, String> identMap = new HashMap<>();
        String aktørID = fnr; // test data har aktørID = fnr

        List<Fagsak> saker = fagsakRepository.findByRolleAndAktør(RolleType.BRUKER, aktørID);

        return tilDtoer(saker);
    }

    @GET
    @Path("{saksnr}")
    @ApiOperation(value = "Henter en sak med et gitt saksnummer", notes = ("Spesifikke saker kan hentes via saksnummer."))
    public Response hentFagsak(@PathParam("saksnr") @ApiParam("Saksnummer.") Long saksnummer) {
        Fagsak sak = fagsakRepository.findBySaksnummer(saksnummer);

        if (sak == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            FagsakDto fagsakDto = tilDto(sak);
            return Response.ok(fagsakDto).build();
        }

    }

    private List<FagsakOppsummeringDto> tilDtoer(List<Fagsak> saker) {
        List<FagsakOppsummeringDto> fagsakListe = new ArrayList<>();

        for (Fagsak  fagsak : saker) {
            FagsakOppsummeringDto fagsakOppsummeringDto = new FagsakOppsummeringDto();
            modelMapper.map(fagsak, fagsakOppsummeringDto);

            // FIXME Er datamodellen riktig her?
            if (fagsak.getBehandlinger() != null && fagsak.getBehandlinger().size() > 0) {
                Behandling behandling = fagsak.getBehandlinger().get(0);
                Set<Saksopplysning> saksopplysninger = behandling.getSaksopplysninger();

                Set<Aktoer> aktører = fagsak.getAktører();
                Optional<Aktoer> bruker = aktører.stream().filter(a -> a.getRolle().equals(RolleType.BRUKER)).findFirst();
                if (bruker.isPresent()) {
                    fagsakOppsummeringDto.setFnr(bruker.get().getEksternId());
                }

                Optional<Saksopplysning> opt = saksopplysninger.stream().filter(s -> s.getType().equals(SaksopplysningType.PERSONOPPLYSNING)).findFirst();
                if (opt.isPresent()) {
                    PersonopplysningDokument dokument = (PersonopplysningDokument) dokumentFactory.lagDokument(opt.get());
                    fagsakOppsummeringDto.setKjønn(dokument.kjønn);
                    fagsakOppsummeringDto.setSammensattNavn(dokument.sammensattNavn);
                }
            }

            fagsakListe.add(fagsakOppsummeringDto);
        }

        return fagsakListe;
    }

    private FagsakDto tilDto(Fagsak fagsak) {
        FagsakDto fagsakDto = new FagsakDto();
        modelMapper.map(fagsak, fagsakDto);
        return fagsakDto;
    }

}
