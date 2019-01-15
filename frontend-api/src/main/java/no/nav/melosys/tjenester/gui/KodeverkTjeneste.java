package no.nav.melosys.tjenester.gui;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import io.swagger.annotations.Api;
import no.nav.melosys.domain.begrunnelse.SokkelEllerSkip;
import no.nav.melosys.domain.dokument.arbeidsforhold.Fartsomraade;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.service.kodeverk.KodeDto;
import no.nav.melosys.tjenester.gui.dto.kodeverk.KodeverkDto;
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
        kodeverk.getBegrunnelser().artikkel12_1 = tilKoder(Art12_1_Begrunnelser.values());
        kodeverk.getBegrunnelser().artikkel12_2 = tilKoder(Art12_2_Begrunnelser.values());
        kodeverk.getBegrunnelser().artikkel16_1_anmodning = tilKoder(Art16_1_Anmodning_Begrunnelser.values());
        kodeverk.getBegrunnelser().bosted = tilKoder(Bosted.values());
        kodeverk.getBegrunnelser().forutgaendeMedlemskap = tilKoder(ForutgaendeMedlemskap.values());
        kodeverk.getBegrunnelser().sokkelEllerSkip = tilKoder(SokkelEllerSkip.values());
        kodeverk.getBegrunnelser().normaltDriverVirksomhet = tilKoder(NormaltDriverVirksomhet.values());
        kodeverk.getBegrunnelser().opphold = tilKoder(Opphold.values());
        kodeverk.getBegrunnelser().vesentligVirksomhet = tilKoder(VesentligVirksomhet.values());
        kodeverk.getBehandlinger().behandlingsstatus = tilKoder(Behandlingsstatus.values());
        kodeverk.getBehandlinger().behandlingstyper = tilKoder(Behandlingstyper.values());
        kodeverk.getBehandlinger().behandlingsresultattyper = tilKoder(Behandlingsresultattyper.values());
        kodeverk.getBrev().produserbareDokumenter = tilKoder(ProduserbartDokument.values());
        kodeverk.getLovvalgsbestemmelser().forordning_883_2004 = tilKoder(LovvalgsBestemmelser_883_2004.values());
        kodeverk.getLovvalgsbestemmelser().forordning_987_2009 = tilKoder(LovvalgsBestemmelser_987_2009.values());
        kodeverk.getLovvalgsbestemmelser().tillegg = tilKoder(TilleggsBestemmelser_883_2004.values());
        kodeverk.getYrker().yrkesaktivitetstyper = tilKoder(Yrkesaktivitetstyper.values());
        kodeverk.getYrker().yrkesgrupper = tilKoder(Yrkesgrupper.values());
        kodeverk.setAktoerroller(tilKoder(Aktoerroller.values()));
        kodeverk.setDokumenttitler(tilKoder(Dokumenttitler.values()));
        kodeverk.setFartsomrader(tilKoder(Fartsomraade.values()));
        kodeverk.setFinansiering(tilKoder(Finansiering.values()));
        kodeverk.setLandkoder(tilKoder(Landkoder.values()));
        kodeverk.setMedlemskapstyper(tilKoder(Medlemskapstyper.values()));
        kodeverk.setMottaksretning(tilKoder(Mottaksretning.values()));
        kodeverk.setOppgavetyper(tilKoder(Oppgavetyper.values()));
        kodeverk.setRepresenterer(tilKoder(Representerer.values()));
        kodeverk.setSaksstatuser(tilKoder(Saksstatuser.values()));
        kodeverk.setSakstyper(tilKoder(Sakstyper.values()));
        kodeverk.setTrygdedekninger(tilKoder(Trygdedekninger.values()));
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
