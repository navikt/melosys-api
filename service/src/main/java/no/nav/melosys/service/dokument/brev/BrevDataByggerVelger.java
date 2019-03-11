package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.kodeverk.Produserbaredokumenter;
import no.nav.melosys.integrasjon.joark.JoarkService;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.RegisterOppslagSystemService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.bygger.*;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BrevDataByggerVelger {

    private final AvklartefaktaService avklartefaktaService;
    private final KodeverkService kodeverkService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;
    private final VilkaarsresultatRepository vilkaarsresultatRepository;
    private final JoarkService joarkService;
    private final AvklarteVirksomheterService avklarteVirksomheterService;
    private final BehandlingsresultatService behandlingsresultatService;

    @Autowired
    public BrevDataByggerVelger(AvklartefaktaService avklartefaktaService,
                                RegisterOppslagSystemService registerOppslagService,
                                KodeverkService kodeverkService,
                                LovvalgsperiodeService lovvalgsperiodeService,
                                UtenlandskMyndighetRepository utenlandskMyndighetRepository,
                                VilkaarsresultatRepository vilkaarsresultatRepository,
                                JoarkService joarkService, BehandlingsresultatService behandlingsresultatService) {
        this.avklartefaktaService = avklartefaktaService;
        this.kodeverkService = kodeverkService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
        this.joarkService = joarkService;
        this.avklarteVirksomheterService = new AvklarteVirksomheterService(avklartefaktaService, registerOppslagService);
        this.behandlingsresultatService = behandlingsresultatService;
    }

    // For brevbygging i saksflyt
    public BrevDataBygger hent(Produserbaredokumenter produserbartDokument) {
        return hent(produserbartDokument, new BrevbestillingDto());
    }

    public BrevDataBygger hent(Produserbaredokumenter produserbartDokument, BrevbestillingDto brevbestillingDto) {
        switch (produserbartDokument) {
            case ATTEST_A1: {
                BrevDataByggerA1 a1Bygger =
                    new BrevDataByggerA1(avklartefaktaService,
                        avklarteVirksomheterService,
                        kodeverkService);
                return new BrevDataByggerVedlegg(a1Bygger, brevbestillingDto);
            }
            case AVSLAG_ARBEIDSGIVER:
                return new BrevDataByggerAvslagArbeidsgiver(avklartefaktaService,
                                                            avklarteVirksomheterService,
                                                            lovvalgsperiodeService,
                                                            vilkaarsresultatRepository);
            case AVSLAG_YRKESAKTIV:
            case ORIENTERING_ANMODNING_UNNTAK: {
                return new BrevDataByggerAnmodningUnntakOgAvslag(avklartefaktaService, avklarteVirksomheterService, behandlingsresultatService);
            }
            case ANMODNING_UNNTAK: {
                BrevDataByggerA001 a001Bygger =
                    new BrevDataByggerA001(avklartefaktaService,
                        avklarteVirksomheterService,
                        kodeverkService,
                        lovvalgsperiodeService,
                        utenlandskMyndighetRepository,
                        vilkaarsresultatRepository);
                return new BrevDataByggerVedlegg(a001Bygger, brevbestillingDto);
            }
            case INNVILGELSE_YRKESAKTIV: {
                BrevDataByggerA1 a1Bygger =
                    new BrevDataByggerA1(avklartefaktaService,
                        avklarteVirksomheterService,
                        kodeverkService);
                return new BrevDataByggerVedlegg(a1Bygger, brevbestillingDto);
            }
            case MELDING_HENLAGT_SAK: {
                return new BrevDataByggerHenleggelse(joarkService, brevbestillingDto);
            }
            default:
                return new BrevDataByggerStandard(brevbestillingDto);
        }
    }
}