package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.repository.LovvalgsperiodeRepository;
import no.nav.melosys.repository.TidligereMedlemsperiodeRepository;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.repository.VilkaarsresultatRepository;
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
    private final KodeverkService kodeverkService;
    private TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository;
    private UtenlandskMyndighetRepository utenlandskMyndighetRepository;
    private LovvalgsperiodeRepository lovvalgsperiodeRepository;
    private VilkaarsresultatRepository vilkaarsresultatRepository;

    @Autowired
    public BrevDataByggerVelger(AvklartefaktaService avklartefaktaService,
                                RegisterOppslagSystemService registerOppslagService,
                                KodeverkService kodeverkService,
                                TidligereMedlemsperiodeRepository tidligereMedlemsperiodeRepository,
                                UtenlandskMyndighetRepository utenlandskMyndighetRepository,
                                LovvalgsperiodeRepository lovvalgsperiodeRepository,
                                VilkaarsresultatRepository vilkaarsresultatRepository) {
        this.avklartefaktaService = avklartefaktaService;
        this.registerOppslagService = registerOppslagService;
        this.kodeverkService = kodeverkService;
        this.tidligereMedlemsperiodeRepository = tidligereMedlemsperiodeRepository;
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
        this.lovvalgsperiodeRepository = lovvalgsperiodeRepository;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
    }

    // For brevbygging i saksflyt
    public BrevDataBygger hent(DokumentType dokumentType) {
        return hent(dokumentType, new BrevbestillingDto());
    }

    public BrevDataBygger hent(DokumentType dokumentType, BrevbestillingDto brevbestillingDto) {
        switch (dokumentType) {
            case ATTEST_A1: {
                BrevDataByggerA1 a1Bygger =
                        new BrevDataByggerA1(avklartefaktaService,
                                registerOppslagService,
                                kodeverkService);
                return new BrevDataByggerVedlegg(a1Bygger, brevbestillingDto);
            }
            case ATTEST_A001: {
                BrevDataByggerA001 a001Bygger =
                        new BrevDataByggerA001(avklartefaktaService,
                                registerOppslagService,
                                kodeverkService,
                                tidligereMedlemsperiodeRepository,
                                utenlandskMyndighetRepository,
                                lovvalgsperiodeRepository,
                                vilkaarsresultatRepository);
                return new BrevDataByggerVedlegg(a001Bygger, brevbestillingDto);
            }
            case INNVILGELSE_YRKESAKTIV: {
                BrevDataByggerA1 a1Bygger =
                        new BrevDataByggerA1(avklartefaktaService,
                                registerOppslagService,
                                kodeverkService);
                return new BrevDataByggerVedlegg(a1Bygger, brevbestillingDto);
            }
            default:
                return new BrevDataByggerStandard(brevbestillingDto);
        }
    }
}