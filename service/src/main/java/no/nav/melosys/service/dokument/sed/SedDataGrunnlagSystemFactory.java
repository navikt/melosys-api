package no.nav.melosys.service.dokument.sed;

import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Qualifier("system")
public class SedDataGrunnlagSystemFactory extends SedDataGrunnlagFactory {

    public SedDataGrunnlagSystemFactory(AvklartefaktaService avklartefaktaService,
                                        AvklarteVirksomheterSystemService avklarteVirksomheterService,
                                        KodeverkService kodeverkService,
                                        @Qualifier("system") PersondataFasade persondataFasade) {
        super(avklartefaktaService, avklarteVirksomheterService, kodeverkService, persondataFasade);
    }
}
