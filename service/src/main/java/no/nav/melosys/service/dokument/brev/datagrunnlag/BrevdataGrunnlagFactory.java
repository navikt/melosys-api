package no.nav.melosys.service.dokument.brev.datagrunnlag;

import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.persondata.PersondataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BrevdataGrunnlagFactory {
    private final AvklartefaktaService avklartefaktaService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final KodeverkService kodeverkService;
    private final PersondataFasade persondataFasade;

    @Autowired
    public BrevdataGrunnlagFactory(AvklartefaktaService avklartefaktaService,
                                   AvklarteVirksomheterSystemService avklarteVirksomheterService,
                                   KodeverkService kodeverkService,
                                   PersondataFasade persondataFasade) {
        this.avklartefaktaService = avklartefaktaService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.kodeverkService = kodeverkService;
        this.persondataFasade = persondataFasade;
    }

    public BrevDataGrunnlag av(DoksysBrevbestilling brevbestilling) {
        return new BrevDataGrunnlag(brevbestilling,
            kodeverkService,
            avklarteVirksomheterService,
            avklartefaktaService,
            persondataFasade);
    }
}
