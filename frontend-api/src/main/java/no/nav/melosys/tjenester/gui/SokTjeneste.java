package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

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
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.service.FagsakService;
import no.nav.melosys.tjenester.gui.dto.FagsakOppsummeringDto;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"sok"})
@Path("/sok")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
@Transactional
public class SokTjeneste extends RestTjeneste {

    private FagsakService fagsakService;

    private DokumentFactory dokumentFactory;

    private ModelMapper modelMapper;

    @Autowired
    public SokTjeneste(FagsakService fagsakService, DokumentFactory dokumentFactory) {
        this.fagsakService = fagsakService;
        this.dokumentFactory = dokumentFactory;

        this.modelMapper = new ModelMapper();
    }

    @GET
    @Path("/fagsaker")
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

    private List<FagsakOppsummeringDto> tilDtoer(Iterable<Fagsak> saker) {
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
                    PersonDokument dokument = (PersonDokument) dokumentFactory.lagDokument(opt.get());
                    fagsakOppsummeringDto.setKjønn(dokument.kjønn);
                    fagsakOppsummeringDto.setSammensattNavn(dokument.sammensattNavn);
                }
            }

            // FIXME saksnummer fra Fagsak bruker id fra DB (midlertidig)
            fagsakOppsummeringDto.setSaksnummer(fagsak.getId());
            fagsakListe.add(fagsakOppsummeringDto);
        }

        return fagsakListe;
    }
}
