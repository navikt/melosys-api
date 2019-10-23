package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.integrasjon.joark.JoarkService;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.bygger.*;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BrevDataByggerVelger {
    private final AnmodningsperiodeService anmodningsperiodeService;
    private final AvklartefaktaService avklartefaktaService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final UtenlandskMyndighetRepository utenlandskMyndighetRepository;
    private final VilkaarsresultatRepository vilkaarsresultatRepository;
    private final JoarkService joarkService;
    private final LandvelgerService landvelgerService;

    @Autowired
    public BrevDataByggerVelger(AnmodningsperiodeService anmodningsperiodeService,
                                AvklartefaktaService avklartefaktaService,
                                LovvalgsperiodeService lovvalgsperiodeService,
                                UtenlandskMyndighetRepository utenlandskMyndighetRepository,
                                VilkaarsresultatRepository vilkaarsresultatRepository,
                                JoarkService joarkService,
                                LandvelgerService landvelgerService) {
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.avklartefaktaService = avklartefaktaService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
        this.joarkService = joarkService;
        this.landvelgerService = landvelgerService;
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
                return new BrevDataByggerAvslagArbeidsgiver(landvelgerService, lovvalgsperiodeService, vilkaarsresultatRepository);
            case AVSLAG_YRKESAKTIV:
            case ORIENTERING_ANMODNING_UNNTAK:
                return new BrevDataByggerAnmodningUnntakOgAvslag(landvelgerService, anmodningsperiodeService, vilkaarsresultatRepository);
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
                                                    anmodningsperiodeService,
                                                    brevbestillingDto);
            case ORIENTERING_VIDERESENDT_SOEKNAD:
                return new BrevDataByggerVideresend(landvelgerService, brevbestillingDto);
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
            new BrevDataByggerA1(avklartefaktaService);
        return new BrevDataByggerVedlegg(a1Bygger, brevbestillingDto);
    }

    private BrevDataBygger lagBrevDataByggerA001(BrevbestillingDto brevbestillingDto) {
        BrevDataByggerA001 a001Bygger =
            new BrevDataByggerA001(lovvalgsperiodeService,
                anmodningsperiodeService,
                utenlandskMyndighetRepository,
                vilkaarsresultatRepository);
        return new BrevDataByggerVedlegg(a001Bygger, brevbestillingDto);
    }

    private BrevDataBygger lagBrevDataByggerInnvilgelse(BrevbestillingDto brevbestillingDto) {
        BrevDataByggerA1 brevbyggerA1 =
            new BrevDataByggerA1(avklartefaktaService);

        return new BrevDataByggerInnvilgelse(avklartefaktaService,
            landvelgerService,
            lovvalgsperiodeService,
            anmodningsperiodeService,
            brevbestillingDto,
            brevbyggerA1);
    }

    private BrevDataBygger lagBrevDataByggerInnvilgelseFlereLand(BrevbestillingDto brevbestillingDto) {
        BrevDataByggerA1 brevbyggerA1 =
            new BrevDataByggerA1(avklartefaktaService);

        return new BrevDataByggerInnvilgelseFlereLand(avklartefaktaService,
            landvelgerService,
            lovvalgsperiodeService,
            brevbestillingDto,
            brevbyggerA1);
    }
}