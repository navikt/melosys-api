package no.nav.melosys.service.dokument.brev.bygger;

import java.util.List;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.avklartefakta.AvklartVirksomhet;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.AbstraktDokumentDataBygger;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataA1;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.Arbeidssted;
import no.nav.melosys.service.dokument.brev.mapper.arbeidssted.FysiskArbeidssted;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.apache.commons.lang3.StringUtils;

public class BrevDataByggerA1 extends AbstraktDokumentDataBygger implements BrevDataBygger {

    private AvklarteVirksomheterService avklarteVirksomheterService;

    public BrevDataByggerA1(AvklartefaktaService avklartefaktaService,
                            AvklarteVirksomheterService avklarteVirksomheterService,
                            KodeverkService kodeverkService) {
        super(kodeverkService, null, avklartefaktaService);
        this.avklarteVirksomheterService = avklarteVirksomheterService;
    }

    @Override
    public BrevData lag(Behandling behandling, String saksbehandler) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        this.behandling = behandling;
        this.søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);
        this.person = SaksopplysningerUtils.hentPersonDokument(behandling);

        BrevDataA1 brevData = new BrevDataA1();
        brevData.yrkesgruppe = avklartefaktaService.hentYrkesGruppe(behandling.getId());
        brevData.utenlandskeVirksomheter = hentUtenlandskeVirksomheter();
        brevData.norskeVirksomheter = avklarteVirksomheterService.hentAlleNorskeVirksomheter(behandling, this::utfyllManglendeAdressefelter);
        brevData.selvstendigeForetak = avklarteVirksomheterService.hentSelvstendigeForetakOrgnumre(behandling);
        brevData.bostedsadresse = hentBostedsadresse();

        // Feltet 5.1 i A1 blander arbeidsgivere og oppdragsgiver.
        // Oppdragsgiver defineres nå for arbeidsstedet, og må utledes derfra
        List<Arbeidssted> arbeidssteder = hentArbeidssteder();
        brevData.arbeidssteder = arbeidssteder;
        brevData.utenlandskeVirksomheter.addAll(hentOppdragsgiverFraArbeidssteder(arbeidssteder));

        brevData.person = person;

        if (brevData.norskeVirksomheter.isEmpty()) {
            throw new TekniskException("Trenger minst en valgt norsk virksomhet for ART12.1");
        }

        // Lev1 kun norske virksomheter som hovedvirksomhet (og kun én)
        brevData.hovedvirksomhet = brevData.norskeVirksomheter.get(0);
        return brevData;
    }

    private List<AvklartVirksomhet> hentOppdragsgiverFraArbeidssteder(List<Arbeidssted> arbeidssteder) {
        return arbeidssteder.stream()
            .filter(this::erFysiskArbeidsstedHosOppdragsgiver)
            .map(FysiskArbeidssted.class::cast)
            .map(this::utledVirksomhetFraArbeidssted)
            .collect(Collectors.toList());
    }

    private boolean erFysiskArbeidsstedHosOppdragsgiver(Arbeidssted arbeidssted) {
        return arbeidssted.erFysisk() &&
            (StringUtils.isNotEmpty(arbeidssted.getNavn()) ||
            StringUtils.isNotEmpty(arbeidssted.getIdnummer()));
    }

    private AvklartVirksomhet utledVirksomhetFraArbeidssted(FysiskArbeidssted arbeidssted) {
        return new AvklartVirksomhet(arbeidssted.getNavn(), arbeidssted.getIdnummer(), arbeidssted.getAdresse(), null);
    }
}
