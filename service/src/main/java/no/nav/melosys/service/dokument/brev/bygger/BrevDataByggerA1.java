package no.nav.melosys.service.dokument.brev.bygger;

import java.util.ArrayList;
import java.util.List;

import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted;
import org.apache.commons.lang3.StringUtils;

public class BrevDataByggerA1 implements BrevDataBygger {
    private final AvklartefaktaService avklartefaktaService;
    private final LandvelgerService landvelgerService;
    private final LovvalgsperiodeService lovvalgsperiodeService;

    public BrevDataByggerA1(AvklartefaktaService avklartefaktaService, LandvelgerService landvelgerService, LovvalgsperiodeService lovvalgsperiodeService) {
        this.avklartefaktaService = avklartefaktaService;
        this.landvelgerService = landvelgerService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) {
        if (dataGrunnlag.getAvklarteVirksomheterGrunnlag().antallVirksomheter() < 1) {
            throw new FunksjonellException("Trenger minst en avklart virksomhet - utenlandsk eller norsk");
        }

        BrevDataA1 brevData = new BrevDataA1();
        brevData.setPerson(dataGrunnlag.getPerson());
        brevData.setYrkesgruppe(avklartefaktaService.finnYrkesGruppe(dataGrunnlag.getBehandling().getId()).orElse(null));
        brevData.setBostedsadresse(dataGrunnlag.getBostedGrunnlag().finnBostedsadresse().orElse(null));

        List<Arbeidssted> arbeidssteder = new ArrayList<>();
        Lovvalgsperiode lovvalgsperiode = lovvalgsperiodeService.hentLovvalgsperiode(dataGrunnlag.getBehandling().getId());

        // For å unngå bestilling av nytt felt i Attest A1 brevet fra CCM,
        // har vi bestemt at vi bare sender en statisk tekst i feltet 5.1.
        if (lovvalgsperiode.erEftaStorbritannia()) {
            StrukturertAdresse placeholderAdresseEfta = new StrukturertAdresse();
            placeholderAdresseEfta.setLandkode("GB");
            FysiskArbeidssted placeholderArbeidsstedEfta = new FysiskArbeidssted("Issued under the EEA EFTA Convention", "", placeholderAdresseEfta);
            FysiskArbeidssted placeholderArbeidsstedEftaLinjeskift = new FysiskArbeidssted(" ", "", placeholderAdresseEfta);
            arbeidssteder.add(placeholderArbeidsstedEfta);
            arbeidssteder.add(placeholderArbeidsstedEftaLinjeskift);
        }

        arbeidssteder.addAll(dataGrunnlag.getArbeidsstedGrunnlag().hentArbeidssteder());
        brevData.setArbeidssteder(arbeidssteder);
        brevData.setArbeidsland(landvelgerService.hentAlleArbeidsland(dataGrunnlag.getBehandling().getId()));
        brevData.setUkjenteEllerAlleEosLand(dataGrunnlag.getMottatteOpplysningerData().soeknadsland.isFlereLandUkjentHvilke());

        brevData.setHovedvirksomhet(dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentHovedvirksomhet());
        brevData.setBivirksomheter(dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentBivirksomheter());


        // Feltet 5.1 i A1 fletter arbeidsgivere og oppdragsgivere.
        // Oppdragsgiver defineres for arbeidsstedet og må utledes derfra
        brevData.getBivirksomheter().addAll(hentForetakFraArbeidssteder(arbeidssteder));
        return brevData;
    }

    // Oppdragsgiver kan oppgis rett på det fysiske arbeidsstedet,
    // men skal vises i listen (5.1) sammen med andre utenlandske foretak (utenlandske arbeidsgivere/selvstendig næringsdrivende)
    private List<AvklartVirksomhet> hentForetakFraArbeidssteder(List<Arbeidssted> arbeidssteder) {
        return arbeidssteder.stream()
            .filter(this::arbeidsstedHarForetak)
            .map(this::utledVirksomhetFraArbeidssted)
            .toList();
    }

    private boolean arbeidsstedHarForetak(Arbeidssted arbeidssted) {
        return StringUtils.isNotEmpty(arbeidssted.getForetakNavn()) ||
            StringUtils.isNotEmpty(arbeidssted.getIdnummer());
    }

    private AvklartVirksomhet utledVirksomhetFraArbeidssted(Arbeidssted arbeidssted) {
        return new AvklartVirksomhet(arbeidssted.getForetakNavn(), arbeidssted.getIdnummer(), null, null);
    }
}
