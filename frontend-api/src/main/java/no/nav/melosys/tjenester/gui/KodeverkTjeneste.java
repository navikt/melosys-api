package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.begrunnelse.*;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_883_2004;
import no.nav.melosys.domain.bestemmelse.LovvalgBestemmelse_987_2009;
import no.nav.melosys.domain.bestemmelse.TilleggBestemmelse_883_2004;
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
        kodeverk.getBegrunnelser().artikkel12_1 = tilKoder(Artikkel12_1.values());
        kodeverk.getBegrunnelser().artikkel16_1_anmodning = tilKoder(Artikkel16_1_Anmodning.values());
        kodeverk.getBegrunnelser().bosted = tilKoder(Bosted.values());
        kodeverk.getBegrunnelser().forutgaendeMedlemskap = tilKoder(ForutgaaendeMedlemskap.values());
        kodeverk.getBegrunnelser().ikkeSkip = tilKoder(IkkeSkip.values());
        kodeverk.getBegrunnelser().opphold = tilKoder(Opphold.values());
        kodeverk.getBegrunnelser().vesentligVirksomhet = tilKoder(VesentligVirksomhet.values());
        kodeverk.setAktoerroller(tilKoder(RolleType.values()));
        kodeverk.setBehandlingsstatus(tilKoder(Behandlingsstatus.values()));
        kodeverk.setBehandlingstyper(tilKoder(Behandlingstype.values()));
        kodeverk.setDokumenttitler(tilKoder(DokumentTittel.values()));
        kodeverk.setDokumenttyper(tilKoder(Dokumenttype.values()));
        kodeverk.setFinansiering(tilKoder(Finansiering.values()));
        kodeverk.setLandkoder(tilKoder(Landkoder.values()));
        kodeverk.getLovvalgsbestemmelser().forordning_883_2004 = tilKoder(LovvalgBestemmelse_883_2004.values());
        kodeverk.getLovvalgsbestemmelser().forordning_987_2009 = tilKoder(LovvalgBestemmelse_987_2009.values());
        kodeverk.getLovvalgsbestemmelser().tillegg = tilKoder(TilleggBestemmelse_883_2004.values());
        // FIXME kodeverk ikke opprettet
        kodeverk.setLovvalgsunntak(tilKoder(LovvalgBestemmelse_883_2004.values()));
        kodeverk.setOppgavetyper(tilKoder(Oppgavetype.values()));
        kodeverk.setRepresenterer(tilKoder(Representerer.values()));
        kodeverk.setSakstyper(tilKoder(Fagsakstype.values()));
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
