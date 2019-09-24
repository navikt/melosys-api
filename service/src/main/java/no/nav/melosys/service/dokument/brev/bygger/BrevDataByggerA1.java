package no.nav.melosys.service.dokument.brev.bygger;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;
import org.apache.commons.lang3.StringUtils;

public class BrevDataByggerA1 implements BrevDataBygger {
    private final AvklartefaktaService avklartefaktaService;

    public BrevDataByggerA1(AvklartefaktaService avklartefaktaService) {
        this.avklartefaktaService = avklartefaktaService;
    }

    @Override
    public BrevData lag(BrevDataGrunnlag dataGrunnlag, String saksbehandler) throws FunksjonellException, TekniskException {
        List<AvklartVirksomhet> utenlandskeVirksomheter = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentUtenlandskeVirksomheter();
        List<AvklartVirksomhet> norskeVirksomheter = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentAlleNorskeVirksomheterMedAdresse();
        if (norskeVirksomheter.isEmpty() && utenlandskeVirksomheter.isEmpty()) {
            throw new FunksjonellException("Trenger minst en avklart virksomhet - utenlandsk eller norsk");
        }

        BrevDataA1 brevData = new BrevDataA1();
        brevData.person = dataGrunnlag.getPerson();
        brevData.yrkesgruppe = avklartefaktaService.hentYrkesGruppe(dataGrunnlag.getBehandling().getId());
        brevData.selvstendigeForetak = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentNorskeSelvstendigeForetakOrgnumre();
        brevData.bostedsadresse = dataGrunnlag.getBostedGrunnlag().hentBostedsadresse();

        List<Arbeidssted> arbeidssteder = dataGrunnlag.getArbeidssteder().hentArbeidssteder();
        brevData.arbeidssteder = arbeidssteder;

        brevData.hovedvirksomhet = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentHovedvirksomhet();
        brevData.bivirksomheter = dataGrunnlag.getAvklarteVirksomheterGrunnlag().hentBivirksomheter();

        // Feltet 5.1 i A1 fletter arbeidsgivere og oppdragsgivere.
        // Oppdragsgiver defineres for arbeidsstedet og må utledes derfra
        brevData.bivirksomheter.addAll(hentForetakFraArbeidssteder(arbeidssteder));
        return brevData;
    }

    // Oppdragsgiver kan oppgis rett på det fysiske arbeidsstedet,
    // men skal vises i listen (5.1) sammen med andre utenlandske foretak (utenlandske arbeidsgivere/selvstendig næringsdrivende)
    private List<AvklartVirksomhet> hentForetakFraArbeidssteder(List<Arbeidssted> arbeidssteder) {
        return arbeidssteder.stream()
            .filter(this::arbeidsstedHarForetak)
            .map(this::utledVirksomhetFraArbeidssted)
            .collect(Collectors.toList());
    }

    private boolean arbeidsstedHarForetak(Arbeidssted arbeidssted) {
        return StringUtils.isNotEmpty(arbeidssted.getNavn()) ||
            StringUtils.isNotEmpty(arbeidssted.getIdnummer());
    }

    private AvklartVirksomhet utledVirksomhetFraArbeidssted(Arbeidssted arbeidssted) {
        return new AvklartVirksomhet(arbeidssted.getNavn(), arbeidssted.getIdnummer(), null, null);
    }
}