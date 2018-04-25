package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.RolleType;
import no.nav.melosys.domain.SaksopplysningType;
import no.nav.melosys.domain.dokument.SaksopplysningDokument;
import no.nav.melosys.domain.dokument.soeknad.Periode;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.service.FagsakService;
import no.nav.melosys.tjenester.gui.dto.FagsakOppsummeringDto;
import no.nav.melosys.tjenester.gui.dto.PeriodeDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

import static no.nav.melosys.domain.util.SaksopplysningerUtil.hentDokument;
import static no.nav.melosys.domain.util.SoeknadUtil.hentLand;
import static no.nav.melosys.domain.util.SoeknadUtil.hentPeriode;

@Api(tags = {"sok"})
@Path("/sok")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class SokTjeneste extends RestTjeneste {

    private FagsakService fagsakService;

    @Autowired
    public SokTjeneste(FagsakService fagsakService) {
        this.fagsakService = fagsakService;
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

        for (Fagsak fagsak : saker) {
            FagsakOppsummeringDto fagsakOppsummeringDto = new FagsakOppsummeringDto();
            // FIXME saksnummer bruker id fra DB
            fagsakOppsummeringDto.setSaksnummer("" + fagsak.getId());
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
                    fagsakOppsummeringDto.setSøknadsperiode(new PeriodeDto(periode.getFom(), periode.getTom()));
                }
            }

            fagsakListe.add(fagsakOppsummeringDto);
        }

        return fagsakListe;
    }
}
