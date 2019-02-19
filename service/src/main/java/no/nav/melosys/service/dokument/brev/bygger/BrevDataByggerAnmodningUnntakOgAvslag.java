package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Set;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntakOgAvslag;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;

public class BrevDataByggerAnmodningUnntakOgAvslag implements BrevDataBygger {

    private final AvklartefaktaService avklartefaktaService;
    private final RegisterOppslagService registerOppslagService;

    public BrevDataByggerAnmodningUnntakOgAvslag(AvklartefaktaService avklartefaktaService,
                                                 RegisterOppslagService registerOppslagService) {
        this.avklartefaktaService = avklartefaktaService;
        this.registerOppslagService = registerOppslagService;
    }

    @Override
    public BrevData lag(Behandling behandling, String saksbehandler) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {

        Set<String> avklarteOrganisasjoner = avklartefaktaService.hentAvklarteOrganisasjoner(behandling.getId());

        Virksomhet hovedvirksomhet = registerOppslagService.hentOrganisasjoner(avklarteOrganisasjoner).stream()
            .map(org -> new Virksomhet(org.lagSammenslåttNavn(), org.getOrgnummer(), null))
            .findFirst()
            .orElseThrow(() -> new TekniskException("Trenger minst en norsk virksomhet for avslag og ART16.1"));

        avklarSelvstendigForetakVirksomhet(behandling, hovedvirksomhet);

        BrevDataAnmodningUnntakOgAvslag brevData = new BrevDataAnmodningUnntakOgAvslag(saksbehandler);
        brevData.hovedvirksomhet = hovedvirksomhet;
        return brevData;
    }

    private void avklarSelvstendigForetakVirksomhet(Behandling behandling, Virksomhet hovedvirksomhet) throws TekniskException {
        SoeknadDokument soeknadDokument = SaksopplysningerUtils.hentSøknadDokument(behandling);

        if (soeknadDokument.selvstendigArbeid.erSelvstendig
                && soeknadDokument.selvstendigArbeid.selvstendigForetak.stream()
                .anyMatch(selvstendigForetak -> selvstendigForetak.orgnr.equalsIgnoreCase(hovedvirksomhet.orgnr))) {
            hovedvirksomhet.setSelvstendigForetak(true);
        } else {
            hovedvirksomhet.setSelvstendigForetak(false);
        }
    }
}
