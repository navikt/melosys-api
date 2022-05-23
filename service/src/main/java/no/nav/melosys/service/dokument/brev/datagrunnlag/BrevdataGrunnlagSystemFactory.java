package no.nav.melosys.service.dokument.brev.datagrunnlag;

import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.stereotype.Component;

@Component

public class BrevdataGrunnlagSystemFactory extends BrevdataGrunnlagFactory {

    public BrevdataGrunnlagSystemFactory(AvklartefaktaService avklartefaktaService,
                                         AvklarteVirksomheterService avklarteVirksomheterService,
                                         KodeverkService kodeverkService,
                                         PersondataFasade persondataFasade) {
        super(avklartefaktaService, avklarteVirksomheterService, kodeverkService, persondataFasade);
    }
}
