package no.nav.melosys.service.dokument.brev.datagrunnlag;

import no.finn.unleash.Unleash;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("system")
public class BrevdataGrunnlagSystemFactory extends BrevdataGrunnlagFactory {

    @Autowired
    public BrevdataGrunnlagSystemFactory(AvklartefaktaService avklartefaktaService,
                                         AvklarteVirksomheterSystemService avklarteVirksomheterService,
                                         KodeverkService kodeverkService,
                                         @Qualifier("system") PersondataFasade persondataFasade,
                                         Unleash unleash) {
        super(avklartefaktaService, avklarteVirksomheterService, kodeverkService, persondataFasade);
    }
}
