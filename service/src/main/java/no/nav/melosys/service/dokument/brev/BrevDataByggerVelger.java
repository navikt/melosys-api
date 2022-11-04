package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.integrasjon.joark.JoarkService;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService;
import no.nav.melosys.service.dokument.brev.bygger.*;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.utpeking.UtpekingService;
import no.nav.melosys.service.vilkaar.VilkaarsresultatService;
import org.springframework.stereotype.Component;

@Component
public class BrevDataByggerVelger {
    private final AnmodningsperiodeService anmodningsperiodeService;
    private final AvklartefaktaService avklartefaktaService;
    private final JoarkService joarkService;
    private final LandvelgerService landvelgerService;
    private final LovvalgsperiodeService lovvalgsperiodeService;
    private final SaksopplysningerService saksopplysningerService;
    private final UtenlandskMyndighetService utenlandskMyndighetService;
    private final UtpekingService utpekingService;
    private final VilkaarsresultatRepository vilkaarsresultatRepository;
    private final VilkaarsresultatService vilkaarsresultatService;
    private final PersondataFasade persondataFasade;
    private final MottatteOpplysningerService mottatteOpplysningerService;

    public BrevDataByggerVelger(AnmodningsperiodeService anmodningsperiodeService,
                                AvklartefaktaService avklartefaktaService,
                                JoarkService joarkService,
                                LandvelgerService landvelgerService,
                                LovvalgsperiodeService lovvalgsperiodeService,
                                SaksopplysningerService saksopplysningerService,
                                UtenlandskMyndighetService utenlandskMyndighetService,
                                UtpekingService utpekingService,
                                VilkaarsresultatRepository vilkaarsresultatRepository,
                                VilkaarsresultatService vilkaarsresultatService,
                                PersondataFasade persondataFasade,
                                MottatteOpplysningerService mottatteOpplysningerService) {
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.avklartefaktaService = avklartefaktaService;
        this.joarkService = joarkService;
        this.landvelgerService = landvelgerService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.saksopplysningerService = saksopplysningerService;
        this.utenlandskMyndighetService = utenlandskMyndighetService;
        this.utpekingService = utpekingService;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
        this.vilkaarsresultatService = vilkaarsresultatService;
        this.persondataFasade = persondataFasade;
        this.mottatteOpplysningerService = mottatteOpplysningerService;
    }

    public BrevDataBygger hent(Produserbaredokumenter produserbartDokument, BrevbestillingRequest brevbestillingRequest) {
        return switch (produserbartDokument) {
            case ATTEST_A1 -> lagBrevDataByggerA1(brevbestillingRequest);
            case AVSLAG_ARBEIDSGIVER -> new BrevDataByggerAvslagArbeidsgiver(landvelgerService, lovvalgsperiodeService,
                vilkaarsresultatRepository);
            case AVSLAG_YRKESAKTIV ->
                new BrevDataByggerAvslagYrkesaktiv(landvelgerService, anmodningsperiodeService, brevbestillingRequest,
                    vilkaarsresultatService);
            case ORIENTERING_ANMODNING_UNNTAK ->
                new BrevDataByggerAnmodningUnntak(landvelgerService, vilkaarsresultatService);
            case ANMODNING_UNNTAK -> lagBrevDataByggerA001(brevbestillingRequest);
            case INNVILGELSE_YRKESAKTIV -> lagBrevDataByggerInnvilgelse(brevbestillingRequest);
            case INNVILGELSE_YRKESAKTIV_FLERE_LAND -> lagBrevDataByggerInnvilgelseFlereLand(brevbestillingRequest);
            case INNVILGELSE_ARBEIDSGIVER ->
                new BrevDataByggerInnvilgelse(avklartefaktaService, landvelgerService, lovvalgsperiodeService,
                    anmodningsperiodeService, brevbestillingRequest,
                    vilkaarsresultatService, persondataFasade,
                    mottatteOpplysningerService);
            case ORIENTERING_UTPEKING_UTLAND ->
                new BrevDataByggerUtpekingAnnetLand(utpekingService, brevbestillingRequest);
            case ORIENTERING_VIDERESENDT_SOEKNAD ->
                new BrevDataByggerVideresend(landvelgerService, utenlandskMyndighetService,
                    brevbestillingRequest);
            case MELDING_MANGLENDE_OPPLYSNINGER, MELDING_FORVENTET_SAKSBEHANDLINGSTID ->
                new BrevDataByggerMedMottattDato(
                    brevbestillingRequest, joarkService);
            default -> new BrevDataByggerStandard(brevbestillingRequest);
        };
    }

    private BrevDataBygger lagBrevDataByggerA1(BrevbestillingRequest brevbestillingRequest) {
        BrevDataByggerA1 a1Bygger =
            new BrevDataByggerA1(avklartefaktaService, landvelgerService);
        return new BrevDataByggerVedlegg(a1Bygger, brevbestillingRequest);
    }

    private BrevDataBygger lagBrevDataByggerA001(BrevbestillingRequest brevbestillingRequest) {
        BrevDataByggerA001 a001Bygger =
            new BrevDataByggerA001(lovvalgsperiodeService,
                anmodningsperiodeService,
                utenlandskMyndighetService,
                vilkaarsresultatService);
        return new BrevDataByggerVedlegg(a001Bygger, brevbestillingRequest);
    }

    private BrevDataBygger lagBrevDataByggerInnvilgelse(BrevbestillingRequest brevbestillingRequest) {
        BrevDataByggerA1 brevbyggerA1 =
            new BrevDataByggerA1(avklartefaktaService, landvelgerService);
        return new BrevDataByggerInnvilgelse(avklartefaktaService,
            landvelgerService,
            lovvalgsperiodeService,
            anmodningsperiodeService,
            brevbestillingRequest,
            brevbyggerA1,
            vilkaarsresultatService,
            persondataFasade,
            mottatteOpplysningerService);
    }

    private BrevDataBygger lagBrevDataByggerInnvilgelseFlereLand(BrevbestillingRequest brevbestillingRequest) {
        BrevDataByggerA1 brevbyggerA1 =
            new BrevDataByggerA1(avklartefaktaService, landvelgerService);

        return new BrevDataByggerInnvilgelseFlereLand(avklartefaktaService,
            landvelgerService,
            lovvalgsperiodeService,
            saksopplysningerService,
            brevbestillingRequest,
            brevbyggerA1);
    }
}
