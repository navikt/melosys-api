package no.nav.melosys.tjenester.gui;

import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.integrasjon.aareg.AaregFasade;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.felles.exception.IntegrasjonException;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.medl.Medl2Fasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import no.nav.melosys.tjenester.gui.dto.FagsakDto;

import java.util.*;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import no.nav.tjeneste.virksomhet.person.v3.informasjon.Informasjonsbehov;
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
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.RolleType;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.dokument.DokumentFactory;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.tjenester.gui.dto.BehandlingDto;

@Api(tags = {"fagsak"})
@Path("/fagsaker")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class FagsakRestTjeneste extends RestTjeneste {

    private FagsakRepository fagsakRepository;

    private DokumentFactory dokumentFactory;

    private ModelMapper modelMapper;

    private TpsFasade tpsFasade;

    private AaregFasade aaregFasade;

    private EregFasade eregFasade;

    private Medl2Fasade medl2Fasade;

    @Autowired
    public FagsakRestTjeneste(FagsakRepository fagsakRepository, DokumentFactory dokumentFactory, TpsFasade tpsFasade, AaregFasade aaregFasade, EregFasade eregFasade, Medl2Fasade medl2Fasade) {
        this.fagsakRepository = fagsakRepository;
        this.dokumentFactory = dokumentFactory;
        this.fagsakRepository = fagsakRepository;
        this.tpsFasade = tpsFasade;
        this.aaregFasade = aaregFasade;
        this.eregFasade = eregFasade;
        this.medl2Fasade = medl2Fasade;

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
    @Path("{fnr}")
    @ApiOperation(value = "Søk etter saker på fødselsnummer eller d-nummer", notes = ("Saker knyttet til en bruker søkes via fødselsnummer eller d-nummer."))
    public List<FagsakDto> hentFagsaker(@PathParam("fnr") @ApiParam("Fødselsnummer eller D-nummer.")  String fnr) {
        // TODO Oppslag mot TPS for å få aktørID
        Map<String, String> identMap = new HashMap<>();
        identMap.put("", "");

        String aktørID = identMap.get(fnr);

        List<Fagsak> saker = fagsakRepository.findByRolleAndAktør(RolleType.BRUKER, aktørID);

        return tilDto(saker); // TODO Bare en liste av saksnumre til frontend?
    }

    @GET
    @ApiOperation(value = "Henter en sak med et gitt saksnummer", notes = ("Spesifikke saker kan hentes via saksnummer."))
    public Response hentFagsak(@QueryParam("saksnr") @ApiParam("Saksnummer.") Long saksnummer) {
        Fagsak sak = fagsakRepository.findBySaksnummer(saksnummer);

        if (sak == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            FagsakDto fagsakDto = tilDto(sak);
            return Response.ok(fagsakDto).build();
        }

    }

    @GET
    @Path("ny/{fnr}")
    @ApiOperation(value = "Oppretter en ny sak med et gitt fødselsnummer.", notes = ("Saker knyttet til en bruker søkes via fødselsnummer eller d-nummer."))
    public Response nyFagsak(@PathParam("fnr") @ApiParam("Fødselsnummer.") String fnr) {
        try {
            Set<SaksopplysningDokument> dokumenter = new HashSet<>();

            Saksopplysning personSaksopplysning = hentPerson(fnr);
            if (personSaksopplysning != null) {
                dokumenter.add(dokumentFactory.lagDokument(personSaksopplysning));
            }

            Saksopplysning medlemskapSaksopplysning = hentMedlemskap(fnr);
            if (medlemskapSaksopplysning != null) {
                dokumenter.add(dokumentFactory.lagDokument(medlemskapSaksopplysning));
            }

            BehandlingDto behandlingDto = new BehandlingDto().withSaksopplysninger(dokumenter);
            FagsakDto fagsakDto = new FagsakDto().withBehandlinger(Collections.singletonList(behandlingDto));
            fagsakDto.setBehandlinger(Collections.singletonList(behandlingDto));

            return Response.ok(fagsakDto).build();

        } catch (SikkerhetsbegrensningException e) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
    }

    private List<FagsakDto> tilDto(List<Fagsak> fagsaker) {
        List<FagsakDto> dtoer = new ArrayList<>();

        for (Fagsak fagsak : fagsaker) {
            FagsakDto fagsakDto = tilDto(fagsak);
            dtoer.add(fagsakDto);
        }

        return dtoer;
    }

    private FagsakDto tilDto(Fagsak fagsak) {
        FagsakDto fagsakDto = new FagsakDto();
        modelMapper.map(fagsak, fagsakDto);
        return fagsakDto;
    }

    private Saksopplysning hentPerson(String fnr) throws SikkerhetsbegrensningException {
        // TODO: Informasjonsbehov.FAMILIERELASJONER kommer i runde 2
        final List<Informasjonsbehov> informasjonsbehov = Collections.singletonList(Informasjonsbehov.ADRESSE);
        try {
            return tpsFasade.hentPerson(fnr, informasjonsbehov);
        } catch (IntegrasjonException e) {
            return null;
        }
    }

    private Saksopplysning hentMedlemskap(String fnr) throws SikkerhetsbegrensningException {
        try {
            return medl2Fasade.getPeriodeListe(fnr);
        } catch (IntegrasjonException e) {
            return null;
        }
    }

}
