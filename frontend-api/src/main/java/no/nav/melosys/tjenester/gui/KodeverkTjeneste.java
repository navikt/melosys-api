package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.domain.BehandlingType;
import no.nav.melosys.domain.FagsakType;
import no.nav.melosys.domain.Kodeverk;
import no.nav.melosys.domain.Landkoder;
import no.nav.melosys.domain.Oppgavetype;
import no.nav.melosys.tjenester.gui.dto.KodeverdiDto;
import no.nav.melosys.tjenester.gui.dto.KodeverkDto;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.web.context.WebApplicationContext;

@Api(tags = {"kodeverk"})
@Path("/kodeverk")
@Service
@Scope(value= WebApplicationContext.SCOPE_REQUEST)
public class KodeverkTjeneste extends RestTjeneste {

    @GET
    public KodeverkDto getKodeverk() {
        KodeverkDto kodeverk = new KodeverkDto();
        kodeverk.setBehandlingsstatus(tilKodeverdiListe(BehandlingStatus.values()));
        kodeverk.setBehandlingstyper(tilKodeverdiListe(BehandlingType.values()));
        kodeverk.setLandkoder(tilKodeverdiListe(Landkoder.values()));
        kodeverk.setOppgavetyper(tilKodeverdiListe(Oppgavetype.values()));
        kodeverk.setSakstyper(tilKodeverdiListe(FagsakType.values()));
        return kodeverk;
    }

    private List<KodeverdiDto> tilKodeverdiListe(Kodeverk[] kodeverk) {
        List<KodeverdiDto> kodeverkListe = new ArrayList<>();
        for (Kodeverk k : kodeverk) {
            kodeverkListe.add(new KodeverdiDto(k.getKode(), k.getBeskrivelse()));
        }
        return kodeverkListe;
    }
}
