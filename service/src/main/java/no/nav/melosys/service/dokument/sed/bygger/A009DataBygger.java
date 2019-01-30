package no.nav.melosys.service.dokument.sed.bygger;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.RegisterOppslagService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.sed.A009Data;
import no.nav.melosys.service.dokument.sed.AbstraktSedData;
import no.nav.melosys.service.kodeverk.KodeverkService;

public class A009DataBygger extends AbstraktSedDataBygger {

    public A009DataBygger(KodeverkService kodeverkService, RegisterOppslagService registerOppslagService,
                          LovvalgsperiodeService lovvalgsperiodeService, AvklartefaktaService avklartefaktaService) {
        super(kodeverkService, registerOppslagService, lovvalgsperiodeService, avklartefaktaService);
    }

    public AbstraktSedData lag(Behandling behandling) throws TekniskException, FunksjonellException {

        A009Data data = lag(behandling, new A009Data());
        data.setLovvalgsperioder(hentLovvalgsperiode());

        return data;
    }
}
