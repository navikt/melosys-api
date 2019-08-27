package no.nav.melosys.service.dokument.brev.bygger;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;
import no.nav.melosys.service.dokument.brev.ressurser.Brevressurser;
import org.apache.commons.lang3.StringUtils;

public class BrevDataByggerA1 implements BrevDataBygger {
    private final AvklartefaktaService avklartefaktaService;
    private final Brevressurser brevressurser;

    public BrevDataByggerA1(Brevressurser brevressurser,
                            AvklartefaktaService avklartefaktaService) {
        this.avklartefaktaService = avklartefaktaService;
        this.brevressurser = brevressurser;
    }

    @Override
    public BrevData lag(String saksbehandler) throws FunksjonellException, TekniskException {
        List<AvklartVirksomhet> utenlandskeVirksomheter = brevressurser.getAvklarteVirksomheter().hentUtenlandskeVirksomheter();
        List<AvklartVirksomhet> norskeVirksomheter = brevressurser.getAvklarteVirksomheter().hentAlleNorskeVirksomheterMedAdresse();
        if (norskeVirksomheter.isEmpty() && utenlandskeVirksomheter.isEmpty()) {
            throw new FunksjonellException("Trenger minst en avklart virksomhet - utenlandsk eller norsk");
        }

        BrevDataA1 brevData = new BrevDataA1();
        brevData.person = brevressurser.getPerson();
        brevData.yrkesgruppe = avklartefaktaService.hentYrkesGruppe(brevressurser.getBehandling().getId());
        brevData.selvstendigeForetak = brevressurser.getAvklarteVirksomheter().hentNorskeSelvstendigeForetakOrgnumre();
        brevData.bostedsadresse = brevressurser.getBosted().hentBostedsadresse();

        List<Arbeidssted> arbeidssteder = brevressurser.getArbeidssteder().hentArbeidssteder();
        brevData.arbeidssteder = arbeidssteder;

        brevData.hovedvirksomhet = brevressurser.getAvklarteVirksomheter().hentHovedvirksomhet();
        brevData.bivirksomheter = brevressurser.getAvklarteVirksomheter().hentBivirksomheter();

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