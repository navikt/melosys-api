package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.begrunnelser.IkkeSkip;
import no.nav.melosys.domain.begrunnelser.Opphold;
import no.nav.melosys.domain.begrunnelser.VesentligVirksomhet;
import no.nav.melosys.domain.oppgave.Oppgavetype;
import no.nav.melosys.service.kodeverk.KodeDto;
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
        kodeverk.getBegrunnelser().vesentligVirksomhet = tilKoder(VesentligVirksomhet.values());
        kodeverk.getBegrunnelser().ikkeSkip = tilKoder(IkkeSkip.values());
        kodeverk.getBegrunnelser().opphold = tilKoder(Opphold.values());
        kodeverk.setBehandlingsstatus(tilKoder(BehandlingStatus.values()));
        kodeverk.setBehandlingstyper(tilKoder(Behandlingstype.values()));
        kodeverk.setDokumenttitler(tilKoder(DokumentTittel.values()));
        kodeverk.setFinansiering(tilKoder(Finansiering.values()));
        kodeverk.setLandkoder(tilKoder(Landkoder.values()));
        kodeverk.setOppgavetyper(tilKoder(Oppgavetype.values()));
        kodeverk.setSakstyper(tilKoder(FagsakType.values()));
        kodeverk.setVedleggstitler(tilKoder(VedleggTittel.values()));
        return kodeverk;
    }

    private List<KodeDto> tilKoder(Kodeverk[] kodeverk) {
        List<KodeDto> kodeListe = new ArrayList<>();
        for (Kodeverk k : kodeverk) {
            kodeListe.add(new KodeDto(k.getKode(), k.getBeskrivelse()));
        }
        return kodeListe;
    }
}
