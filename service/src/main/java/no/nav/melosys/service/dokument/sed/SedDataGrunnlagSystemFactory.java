package no.nav.melosys.service.dokument.sed;

import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.springframework.stereotype.Component;

@Component
public class SedDataGrunnlagSystemFactory extends SedDataGrunnlagFactory {

    public SedDataGrunnlagSystemFactory(AvklartefaktaService avklartefaktaService,
                                        AvklarteVirksomheterSystemService avklarteVirksomheterService,
                                        KodeverkService kodeverkService,
                                        PersondataFasade persondataFasade) {
        super(avklartefaktaService, avklarteVirksomheterService, kodeverkService, persondataFasade);
    }
}
