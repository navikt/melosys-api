package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.RegisterOppslagSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.DokumentType;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BrevDataByggerVelger {

    private final AvklartefaktaService avklartefaktaService;
    private final RegisterOppslagSystemService registerOppslagService;
    private final BehandlingRepository behandlingRepository;
    private final KodeverkService kodeverkService;

    @Autowired
    public BrevDataByggerVelger(AvklartefaktaService avklartefaktaService,
                                RegisterOppslagSystemService registerOppslagService,
                                BehandlingRepository behandlingRepository,
                                KodeverkService kodeverkService) {
        this.avklartefaktaService = avklartefaktaService;
        this.registerOppslagService = registerOppslagService;
        this.behandlingRepository = behandlingRepository;
        this.kodeverkService = kodeverkService;
    }

    // For brevbygging i saksflyt
    public BrevDataBygger hent(DokumentType dokumentType) {
        return hent(dokumentType, new BrevbestillingDto());
    }

    public BrevDataBygger hent(DokumentType dokumentType, BrevbestillingDto brevbestillingDto) {
        switch (dokumentType) {
            case ATTEST_A1:
                return new BrevDataByggerA1(avklartefaktaService,
                                            behandlingRepository,
                                            registerOppslagService,
                                            kodeverkService);
            default:
                return new BrevDataByggerStandard(brevbestillingDto);
        }
    }
}