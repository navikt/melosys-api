package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.ProduserbartDokument;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.RegisterOppslagSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.bygger.*;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BrevDataByggerVelger {

    private final AvklartefaktaService avklartefaktaService;
    private final RegisterOppslagSystemService registerOppslagService;
    private final KodeverkService kodeverkService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;
    private final VilkaarsresultatRepository vilkaarsresultatRepository;

    @Autowired
    public BrevDataByggerVelger(AvklartefaktaService avklartefaktaService,
                                RegisterOppslagSystemService registerOppslagService,
                                KodeverkService kodeverkService,
                                LovvalgsperiodeService lovvalgsperiodeService,
                                UtenlandskMyndighetRepository utenlandskMyndighetRepository,
                                VilkaarsresultatRepository vilkaarsresultatRepository) {
        this.avklartefaktaService = avklartefaktaService;
        this.registerOppslagService = registerOppslagService;
        this.kodeverkService = kodeverkService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
    }

    // For brevbygging i saksflyt
    public BrevDataBygger hent(ProduserbartDokument produserbartDokument) {
        return hent(produserbartDokument, new BrevbestillingDto());
    }

    public BrevDataBygger hent(ProduserbartDokument produserbartDokument, BrevbestillingDto brevbestillingDto) {
        switch (produserbartDokument) {
            case ATTEST_A1: {
                BrevDataByggerA1 a1Bygger =
                    new BrevDataByggerA1(avklartefaktaService,
                        registerOppslagService,
                        kodeverkService);
                return new BrevDataByggerVedlegg(a1Bygger, brevbestillingDto);
            }
            case AVSLAG_YRKESAKTIV:
            case ORIENTERING_ANMODNING_UNNTAK: {
                return new BrevDataByggerAnmodningUnntakOgAvslag(avklartefaktaService,
                    registerOppslagService);
            }
            case SED_A001: {
                BrevDataByggerA001 a001Bygger =
                    new BrevDataByggerA001(avklartefaktaService,
                        registerOppslagService,
                        kodeverkService,
                        lovvalgsperiodeService,
                        utenlandskMyndighetRepository,
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