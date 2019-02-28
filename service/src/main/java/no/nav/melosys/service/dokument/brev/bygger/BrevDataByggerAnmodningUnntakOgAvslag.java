package no.nav.melosys.service.dokument.brev.bygger;

import java.util.function.Function;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.dokument.felles.Adresse;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.util.SaksopplysningerUtils;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.AbstraktDokumentDataBygger;
import no.nav.melosys.service.dokument.AvklarteVirksomheter;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.dokument.brev.BrevDataAnmodningUnntakOgAvslag;

public class BrevDataByggerAnmodningUnntakOgAvslag extends AbstraktDokumentDataBygger implements BrevDataBygger {

    private RegisterOppslagService registerOppslagService;

    public BrevDataByggerAnmodningUnntakOgAvslag(AvklartefaktaService avklartefaktaService,
                                                 RegisterOppslagService registerOppslagService) {
        super(null, null, avklartefaktaService);
        this.registerOppslagService = registerOppslagService;
    }

    Function<OrganisasjonDokument, Adresse> adresseformaterer = org -> null;

    @Override
    public BrevData lag(Behandling behandling, String saksbehandler) throws IkkeFunnetException, SikkerhetsbegrensningException, TekniskException {
        this.behandling = behandling;
        this.søknad = SaksopplysningerUtils.hentSøknadDokument(behandling);

        BrevDataAnmodningUnntakOgAvslag brevData = new BrevDataAnmodningUnntakOgAvslag(saksbehandler);

        AvklarteVirksomheter avklarteVirksomheter = new AvklarteVirksomheter(avklartefaktaService, registerOppslagService, behandling);
        if (avklarteVirksomheter.antall() != 1) {
            throw new TekniskException("Trenger minst en norsk virksomhet for avslag og ART16.1");
        }

        brevData.hovedvirksomhet = avklarteVirksomheter.hentAlleNorskeAvklarteVirksomheter(adresseformaterer).iterator().next();
        avklarteVirksomheter.avklarSelvstendigForetakVirksomhet(brevData.hovedvirksomhet);
        return brevData;
    }
}