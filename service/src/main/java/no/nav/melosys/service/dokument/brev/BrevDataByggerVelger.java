package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.kodeverk.Produserbaredokumenter;
import no.nav.melosys.integrasjon.joark.JoarkService;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.LandvelgerService;
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
    private final LandvelgerService landvelgerService;

    @Autowired
    public BrevDataByggerVelger(AvklartefaktaService avklartefaktaService,
                                AvklarteVirksomheterSystemService avklarteVirksomheterService,
                                KodeverkService kodeverkService,
                                LovvalgsperiodeService lovvalgsperiodeService,
                                UtenlandskMyndighetRepository utenlandskMyndighetRepository,
                                VilkaarsresultatRepository vilkaarsresultatRepository,
                                JoarkService joarkService) {
        this.avklartefaktaService = avklartefaktaService;
        this.kodeverkService = kodeverkService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
        this.joarkService = joarkService;
        this.avklarteVirksomheterService = avklarteVirksomheterService;
        this.landvelgerService = new LandvelgerService(avklartefaktaService, vilkaarsresultatRepository);
    }

    // For brevbygging i saksflyt
    public BrevDataBygger hent(Produserbaredokumenter produserbartDokument) {
        return hent(produserbartDokument, new BrevbestillingDto());
    }

    public BrevDataBygger hent(Produserbaredokumenter produserbartDokument, BrevbestillingDto brevbestillingDto) {
        switch (produserbartDokument) {
            case ATTEST_A1:
                return lagBrevDataByggerA1(brevbestillingDto);
            case AVSLAG_ARBEIDSGIVER:
                return new BrevDataByggerAvslagArbeidsgiver(avklartefaktaService,
                                                            avklarteVirksomheterService,
                                                            landvelgerService,
                                                            lovvalgsperiodeService,
                                                            vilkaarsresultatRepository);
            case AVSLAG_YRKESAKTIV:
            case ORIENTERING_ANMODNING_UNNTAK:
                return new BrevDataByggerAnmodningUnntakOgAvslag(avklartefaktaService, avklarteVirksomheterService, landvelgerService);
            case ANMODNING_UNNTAK:
                return lagBrevDataByggerA001(brevbestillingDto);
            case INNVILGELSE_YRKESAKTIV:
                return lagBrevDataByggerInnvilgelse(brevbestillingDto);
            case INNVILGELSE_YRKESAKTIV_FLERE_LAND:
                return lagBrevDataByggerInnvilgelseFlereLand(brevbestillingDto);
            case INNVILGELSE_ARBEIDSGIVER:
                return new BrevDataByggerInnvilgelse(avklartefaktaService,
                                                    landvelgerService,
                                                    lovvalgsperiodeService,
                                                    brevbestillingDto);
            case MELDING_HENLAGT_SAK:
                return new BrevDataByggerHenleggelse(joarkService, brevbestillingDto);
            case MELDING_MANGLENDE_OPPLYSNINGER:
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID:
                return new BrevDataByggerMedMottattDato(brevbestillingDto, joarkService);
            default:
                return new BrevDataByggerStandard(brevbestillingDto);
        }
    }

    private BrevDataBygger lagBrevDataByggerA1(BrevbestillingDto brevbestillingDto) {
        BrevDataByggerA1 a1Bygger =
            new BrevDataByggerA1(avklartefaktaService,
                avklarteVirksomheterService,
                kodeverkService);
        return new BrevDataByggerVedlegg(a1Bygger, brevbestillingDto);
    }

    private BrevDataBygger lagBrevDataByggerA001(BrevbestillingDto brevbestillingDto) {
        BrevDataByggerA001 a001Bygger =
            new BrevDataByggerA001(avklartefaktaService,
                avklarteVirksomheterService,
                kodeverkService,
                lovvalgsperiodeService,
                utenlandskMyndighetRepository,
                vilkaarsresultatRepository);
        return new BrevDataByggerVedlegg(a001Bygger, brevbestillingDto);
    }

    private BrevDataBygger lagBrevDataByggerInnvilgelse(BrevbestillingDto brevbestillingDto) {
        BrevDataByggerA1 brevbyggerA1 =
            new BrevDataByggerA1(avklartefaktaService, avklarteVirksomheterService, kodeverkService);

        return new BrevDataByggerInnvilgelse(avklartefaktaService,
            landvelgerService,
            lovvalgsperiodeService,
            brevbestillingDto,
            brevbyggerA1);
    }

    private BrevDataBygger lagBrevDataByggerInnvilgelseFlereLand(BrevbestillingDto brevbestillingDto) {
        BrevDataByggerA1 brevbyggerA1 =
            new BrevDataByggerA1(avklartefaktaService, avklarteVirksomheterService, kodeverkService);

        return new BrevDataByggerInnvilgelseFlereLand(avklartefaktaService,
            avklarteVirksomheterService,
            landvelgerService,
            lovvalgsperiodeService,
            brevbestillingDto,
            brevbyggerA1);
    }
}