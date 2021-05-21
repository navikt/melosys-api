package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.integrasjon.joark.JoarkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.behandlingsgrunnlag.BehandlingsgrunnlagService;
import no.nav.melosys.service.dokument.brev.bygger.*;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.utpeking.UtpekingService;
import no.nav.melosys.service.vilkaar.VilkaarsresultatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
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
    private final BehandlingsgrunnlagService behandlingsgrunnlagService;

    @Autowired
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
                                @Qualifier("system") PersondataFasade persondataFasade,
                                BehandlingsgrunnlagService behandlingsgrunnlagService) {
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
        this.behandlingsgrunnlagService = behandlingsgrunnlagService;
    }

    public BrevDataBygger hent(Produserbaredokumenter produserbartDokument, BrevbestillingDto brevbestillingDto) {
        switch (produserbartDokument) {
            case ATTEST_A1:
                return lagBrevDataByggerA1(brevbestillingDto);
            case AVSLAG_ARBEIDSGIVER:
                return new BrevDataByggerAvslagArbeidsgiver(landvelgerService, lovvalgsperiodeService, vilkaarsresultatRepository);
            case AVSLAG_YRKESAKTIV:
                return new BrevDataByggerAvslagYrkesaktiv(landvelgerService, anmodningsperiodeService, brevbestillingDto, vilkaarsresultatService);
            case ORIENTERING_ANMODNING_UNNTAK:
                return new BrevDataByggerAnmodningUnntak(landvelgerService, vilkaarsresultatService);
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
                                                    brevbestillingDto,
                                                    vilkaarsresultatService,
                                                    persondataFasade,
                                                    behandlingsgrunnlagService);
            case ORIENTERING_UTPEKING_UTLAND:
                return new BrevDataByggerUtpekingAnnetLand(utpekingService, brevbestillingDto);
            case ORIENTERING_VIDERESENDT_SOEKNAD:
                return new BrevDataByggerVideresend(landvelgerService, utenlandskMyndighetService, brevbestillingDto);
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
            new BrevDataByggerA1(avklartefaktaService, landvelgerService);
        return new BrevDataByggerVedlegg(a1Bygger, brevbestillingDto);
    }

    private BrevDataBygger lagBrevDataByggerA001(BrevbestillingDto brevbestillingDto) {
        BrevDataByggerA001 a001Bygger =
            new BrevDataByggerA001(lovvalgsperiodeService,
                anmodningsperiodeService,
                utenlandskMyndighetService,
                vilkaarsresultatService);
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
            vilkaarsresultatService,
            persondataFasade,
            behandlingsgrunnlagService);
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