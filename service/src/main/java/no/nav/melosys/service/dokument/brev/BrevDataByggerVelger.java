package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.joark.JoarkService;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.bygger.*;
import no.nav.melosys.service.dokument.brev.ressurser.Brevressurser;
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

    @Autowired
    public BrevDataByggerVelger(AnmodningsperiodeService anmodningsperiodeService,
                                AvklartefaktaService avklartefaktaService,
                                LovvalgsperiodeService lovvalgsperiodeService,
                                UtenlandskMyndighetRepository utenlandskMyndighetRepository,
                                VilkaarsresultatRepository vilkaarsresultatRepository,
                                JoarkService joarkService) {
        this.anmodningsperiodeService = anmodningsperiodeService;
        this.avklartefaktaService = avklartefaktaService;
        this.lovvalgsperiodeService = lovvalgsperiodeService;
        this.utenlandskMyndighetRepository = utenlandskMyndighetRepository;
        this.vilkaarsresultatRepository = vilkaarsresultatRepository;
        this.joarkService = joarkService;
    }

    // For brevbygging i saksflyt
    public BrevDataBygger hent(Produserbaredokumenter produserbartDokument, Brevressurser brevdataressurs) throws TekniskException {
        return hent(produserbartDokument, brevdataressurs, new BrevbestillingDto());
    }

    public BrevDataBygger hent(Produserbaredokumenter produserbartDokument, Brevressurser brevdataressurser, BrevbestillingDto brevbestillingDto) throws TekniskException {
        switch (produserbartDokument) {
            case ATTEST_A1:
                return lagBrevDataByggerA1(brevdataressurser, brevbestillingDto);
            case AVSLAG_ARBEIDSGIVER:
                return new BrevDataByggerAvslagArbeidsgiver(brevdataressurser, vilkaarsresultatRepository);
            case AVSLAG_YRKESAKTIV:
            case ORIENTERING_ANMODNING_UNNTAK:
                return new BrevDataByggerAnmodningUnntakOgAvslag(brevdataressurser);
            case ANMODNING_UNNTAK:
                return lagBrevDataByggerA001(brevdataressurser, brevbestillingDto);
            case INNVILGELSE_YRKESAKTIV:
                return lagBrevDataByggerInnvilgelse(brevdataressurser, brevbestillingDto);
            case INNVILGELSE_YRKESAKTIV_FLERE_LAND:
                return lagBrevDataByggerInnvilgelseFlereLand(brevdataressurser, brevbestillingDto);
            case INNVILGELSE_ARBEIDSGIVER:
                return new BrevDataByggerInnvilgelse(brevdataressurser,
                                                    avklartefaktaService,
                                                    brevbestillingDto);
            case MELDING_HENLAGT_SAK:
                return new BrevDataByggerHenleggelse(brevdataressurser.getBehandling(), joarkService, brevbestillingDto);
            case MELDING_MANGLENDE_OPPLYSNINGER:
            case MELDING_FORVENTET_SAKSBEHANDLINGSTID:
                return new BrevDataByggerMedMottattDato(brevdataressurser.getBehandling(), brevbestillingDto, joarkService);
            default:
                return new BrevDataByggerStandard(brevbestillingDto);
        }
    }

    private BrevDataBygger lagBrevDataByggerA1(Brevressurser brevdataressurs, BrevbestillingDto brevbestillingDto) throws TekniskException {
        BrevDataByggerA1 a1Bygger =
            new BrevDataByggerA1(brevdataressurs, avklartefaktaService);
        return new BrevDataByggerVedlegg(a1Bygger, brevbestillingDto);
    }

    private BrevDataBygger lagBrevDataByggerA001(Brevressurser brevdataressurs, BrevbestillingDto brevbestillingDto) throws TekniskException {
        BrevDataByggerA001 a001Bygger =
            new BrevDataByggerA001(brevdataressurs,
                lovvalgsperiodeService,
                anmodningsperiodeService,
                utenlandskMyndighetRepository,
                vilkaarsresultatRepository);
        return new BrevDataByggerVedlegg(a001Bygger, brevbestillingDto);
    }

    private BrevDataBygger lagBrevDataByggerInnvilgelse(Brevressurser brevdataressurs, BrevbestillingDto brevbestillingDto) throws TekniskException {
        BrevDataByggerA1 brevbyggerA1 =
            new BrevDataByggerA1(brevdataressurs, avklartefaktaService);

        return new BrevDataByggerInnvilgelse(brevdataressurs,
            avklartefaktaService,
            brevbestillingDto,
            brevbyggerA1);
    }

    private BrevDataBygger lagBrevDataByggerInnvilgelseFlereLand(Brevressurser brevdataressurs, BrevbestillingDto brevbestillingDto) throws TekniskException {
        BrevDataByggerA1 brevbyggerA1 =
            new BrevDataByggerA1(brevdataressurs, avklartefaktaService);

        return new BrevDataByggerInnvilgelseFlereLand(brevdataressurs,
            avklartefaktaService,
            brevbestillingDto,
            brevbyggerA1);
    }
}