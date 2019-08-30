package no.nav.melosys.service.dokument.brev.datagrunnlag;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class DokumentdataGrunnlagFactory {
    private final AvklartefaktaService avklartefaktaService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final KodeverkService kodeverkService;

    @Autowired
    public DokumentdataGrunnlagFactory(AvklartefaktaService avklartefaktaService,
                                       AvklarteVirksomheterSystemService avklarteVirksomheterService,
                                       KodeverkService kodeverkService) {
        this.avklartefaktaService = avklartefaktaService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.kodeverkService = kodeverkService;
    }

    public DokumentdataGrunnlag av(Behandling behandling) throws TekniskException {
        return new DokumentdataGrunnlag(behandling, kodeverkService, avklarteVirksomheterService, avklartefaktaService);
    }
}