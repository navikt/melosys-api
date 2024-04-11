package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.bygger.*;
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.utpeking.UtpekingService;
import no.nav.melosys.service.behandling.BehandlingsresultatVilkaarsresultatService;
import org.springframework.stereotype.Component;

@Component
public class BrevDataByggerVelger {
    private final AnmodningsperiodeService anmodningsperiodeService;
    private final AvklartefaktaService avklartefaktaService;
    private final LandvelgerService landvelgerService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final SaksopplysningerService saksopplysningerService;
    private final UtenlandskMyndighetService utenlandskMyndighetService;
    private final UtpekingService utpekingService;
    private final BehandlingsresultatVilkaarsresultatService behandlingsresultatVilkaarsresultatService;
    private final PersondataFasade persondataFasade;
    private final MottatteOpplysningerService mottatteOpplysningerService;

    public BrevDataByggerVelger(AnmodningsperiodeService anmodningsperiodeService,
                                AvklartefaktaService avklartefaktaService,
                                LandvelgerService landvelgerService,
                                LovvalgsperiodeService lovvalgsperiodeService,
                                SaksopplysningerService saksopplysningerService,
                                UtenlandskMyndighetService utenlandskMyndighetService,
                                UtpekingService utpekingService,
                                BehandlingsresultatVilkaarsresultatService behandlingsresultatVilkaarsresultatService,
                                PersondataFasade persondataFasade,
                                MottatteOpplysningerService mottatteOpplysningerService) {
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.avklartefaktaService = avklartefaktaService;
        this.landvelgerService = landvelgerService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.saksopplysningerService = saksopplysningerService;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
        this.utpekingService = utpekingService;
        this.behandlingsresultatVilkaarsresultatService = behandlingsresultatVilkaarsresultatService;
        this.persondataFasade = persondataFasade;
        this.mottatteOpplysningerService = mottatteOpplysningerService;
    }

    public BrevDataBygger hent(Produserbaredokumenter produserbartDokument, BrevbestillingDto brevbestillingDto) {
        return switch (produserbartDokument) {
            case ATTEST_A1 -> lagBrevDataByggerA1(brevbestillingDto);
            case AVSLAG_ARBEIDSGIVER -> new BrevDataByggerAvslagArbeidsgiver(landvelgerService, lovvalgsperiodeService,
                behandlingsresultatVilkaarsresultatService);
            case AVSLAG_YRKESAKTIV -> new BrevDataByggerAvslagYrkesaktiv(landvelgerService, anmodningsperiodeService, brevbestillingDto,
                behandlingsresultatVilkaarsresultatService);
            case ORIENTERING_ANMODNING_UNNTAK -> new BrevDataByggerAnmodningUnntak(landvelgerService, behandlingsresultatVilkaarsresultatService);
            case ANMODNING_UNNTAK -> lagBrevDataByggerA001(brevbestillingDto);
            case INNVILGELSE_YRKESAKTIV -> lagBrevDataByggerInnvilgelse(brevbestillingDto);
            case INNVILGELSE_YRKESAKTIV_FLERE_LAND -> lagBrevDataByggerInnvilgelseFlereLand(brevbestillingDto);
            case INNVILGELSE_ARBEIDSGIVER -> new BrevDataByggerInnvilgelse(avklartefaktaService, landvelgerService, lovvalgsperiodeService,
                anmodningsperiodeService, brevbestillingDto,
                behandlingsresultatVilkaarsresultatService, persondataFasade,
                mottatteOpplysningerService);
            case ORIENTERING_UTPEKING_UTLAND -> new BrevDataByggerUtpekingAnnetLand(utpekingService, brevbestillingDto);
            case ORIENTERING_VIDERESENDT_SOEKNAD -> new BrevDataByggerVideresend(landvelgerService, utenlandskMyndighetService,
                brevbestillingDto);
            default -> new BrevDataByggerStandard(brevbestillingDto);
        };
    }

    private BrevDataBygger lagBrevDataByggerA1(BrevbestillingDto brevbestillingDto) {
        BrevDataByggerA1 a1Bygger =
            new BrevDataByggerA1(avklartefaktaService, landvelgerService);
        return new BrevDataByggerVedlegg(a1Bygger, brevbestillingDto);
    }

    private BrevDataBygger lagBrevDataByggerA001(BrevbestillingDto brevbestillingDto) {
        BrevDataByggerA001 a001Bygger =
            new BrevDataByggerA001(lovvalgsperiodeService,
                anmodningsperiodeService,
                utenlandskMyndighetService,
                behandlingsresultatVilkaarsresultatService);
        return new BrevDataByggerVedlegg(a001Bygger, brevbestillingDto);
    }

    private BrevDataBygger lagBrevDataByggerInnvilgelse(BrevbestillingDto brevbestillingDto) {
        BrevDataByggerA1 brevbyggerA1 =
            new BrevDataByggerA1(avklartefaktaService, landvelgerService);
        return new BrevDataByggerInnvilgelse(avklartefaktaService,
            landvelgerService,
            lovvalgsperiodeService,
            anmodningsperiodeService,
            brevbestillingDto,
            brevbyggerA1,
            behandlingsresultatVilkaarsresultatService,
            persondataFasade,
            mottatteOpplysningerService);
    }

    private BrevDataBygger lagBrevDataByggerInnvilgelseFlereLand(BrevbestillingDto brevbestillingDto) {
        BrevDataByggerA1 brevbyggerA1 =
            new BrevDataByggerA1(avklartefaktaService, landvelgerService);

        return new BrevDataByggerInnvilgelseFlereLand(avklartefaktaService,
            landvelgerService,
            lovvalgsperiodeService,
            saksopplysningerService,
            brevbestillingDto,
            brevbyggerA1);
    }
}
