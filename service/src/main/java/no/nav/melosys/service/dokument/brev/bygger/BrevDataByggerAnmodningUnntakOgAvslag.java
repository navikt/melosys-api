package no.nav.melosys.service.dokument.brev.bygger;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.AbstraktDokumentDataBygger;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntakOgAvslag;
import no.nav.melosys.service.dokument.brev.mapper.felles.Virksomhet;

public class BrevDataByggerAnmodningUnntakOgAvslag extends AbstraktDokumentDataBygger implements BrevDataBygger {

    private final RegisterOppslagService registerOppslagService;

    public BrevDataByggerAnmodningUnntakOgAvslag(AvklartefaktaService avklartefaktaService,
                                                 RegisterOppslagService registerOppslagService) {
        super(null, null, avklartefaktaService);
        this.registerOppslagService = registerOppslagService;
    }

    @Override
    public BrevData lag(Behandling behandling, String saksbehandler) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        this.behandling = behandling;
        this.søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);

        BrevDataAnmodningUnntakOgAvslag brevData = new BrevDataAnmodningUnntakOgAvslag(saksbehandler);

        avklarteOrganisasjoner = avklartefaktaService.hentAvklarteOrganisasjoner(behandling.getId());
        brevData.hovedvirksomhet = registerOppslagService.hentOrganisasjoner(avklarteOrganisasjoner).stream()
            .map(org -> new Virksomhet(org.lagSammenslåttNavn(), org.getOrgnummer(), null))
            .findFirst()
            .orElseThrow(() -> new TekniskException("Trenger minst en norsk virksomhet for avslag og ART16.1"));

        avklarSelvstendigForetakVirksomhet(brevData.hovedvirksomhet);
        return brevData;
    }
}