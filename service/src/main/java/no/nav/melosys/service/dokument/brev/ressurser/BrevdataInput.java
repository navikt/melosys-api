package no.nav.melosys.service.dokument.brev.ressurser;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BrevdataInput {
    private final AvklartefaktaService avklartefaktaService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final VilkaarsresultatRepository vilkaarsresultatRepository;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final KodeverkService kodeverkService;

    @Autowired
    public BrevdataInput(AvklartefaktaService avklartefaktaService,
                         AvklarteVirksomheterSystemService avklarteVirksomheterService,
                         VilkaarsresultatRepository vilkaarsresultatRepository,
                         LovvalgsperiodeService lovvalgsperiodeService,
                         KodeverkService kodeverkService) {
        this.avklartefaktaService = avklartefaktaService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.kodeverkService = kodeverkService;
    }

    public Brevressurser av(Behandling behandling) throws TekniskException {
        LandvelgerService landvelgerService = new LandvelgerService(avklartefaktaService, vilkaarsresultatRepository);
        return new Brevressurser(behandling, kodeverkService, landvelgerService, avklarteVirksomheterService, avklartefaktaService, lovvalgsperiodeService);
    }
}
