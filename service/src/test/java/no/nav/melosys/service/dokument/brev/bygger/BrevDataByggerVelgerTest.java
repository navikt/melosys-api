package no.nav.melosys.service.dokument.brev.bygger;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.BrevDataByggerVelger;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.mottatteopplysninger.MottatteOpplysningerService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.saksopplysninger.SaksopplysningerService;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.utpeking.UtpekingService;
import no.nav.melosys.service.behandling.BehandlingsresultatVilkaarsresultatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class BrevDataByggerVelgerTest {

    private BrevDataByggerVelger brevDataByggerVelger;

    @BeforeEach
    public void setUp() {
        AnmodningsperiodeService anmodningsperiodeService = mock(AnmodningsperiodeService.class);
        AvklartefaktaService avklartefaktaService = mock(AvklartefaktaService.class);
        LandvelgerService landvelgerService = mock(LandvelgerService.class);
        LovvalgsperiodeService lovvalgsperiodeService = mock(LovvalgsperiodeService.class);
        SaksopplysningerService saksopplysningerService = mock(SaksopplysningerService.class);
        UtenlandskMyndighetService utenlandskMyndighetService = mock(UtenlandskMyndighetService.class);
        UtpekingService utpekingService = mock(UtpekingService.class);
        BehandlingsresultatVilkaarsresultatService behandlingsresultatVilkaarsresultatService = mock(BehandlingsresultatVilkaarsresultatService.class);
        PersondataFasade persondataFasade = mock(PersondataFasade.class);
        MottatteOpplysningerService mottatteOpplysningerService = mock(MottatteOpplysningerService.class);

        brevDataByggerVelger = new BrevDataByggerVelger(anmodningsperiodeService, avklartefaktaService,
            landvelgerService, lovvalgsperiodeService, saksopplysningerService, utenlandskMyndighetService,
            utpekingService, behandlingsresultatVilkaarsresultatService, persondataFasade, mottatteOpplysningerService);
    }

    @Test
    void hent_medAttestA1_girVedleggBygger() {
        testHent(Produserbaredokumenter.ATTEST_A1, BrevDataByggerVedlegg.class);
    }

    @Test
    void hent_medSEDA001_girVedleggBygger() {
        testHent(Produserbaredokumenter.ANMODNING_UNNTAK, BrevDataByggerVedlegg.class);
    }

    @Test
    final void hent_InnvilgelsesYrksaktiv_girInnvilgelseBygger() {
        testHent(Produserbaredokumenter.INNVILGELSE_YRKESAKTIV, BrevDataByggerInnvilgelse.class);
    }

    @Test
    final void hent_medDokumentTypeINNVILGELSE_YRKESAKTIV_FLERE_LAND_girBrevDataByggerInnvilgelseFlereLand() {
        testHent(Produserbaredokumenter.INNVILGELSE_YRKESAKTIV_FLERE_LAND, BrevDataByggerInnvilgelseFlereLand.class);
    }

    @Test
    final void hent_InnvilgelsesArbeidsgiver_girInnvilgelseBygger() {
        testHent(Produserbaredokumenter.INNVILGELSE_ARBEIDSGIVER, BrevDataByggerInnvilgelse.class);
    }

    @Test
    final void hent_Avslag_girBrevDataByggerAvslagOgAnmodningUnntak() {
        testHent(Produserbaredokumenter.AVSLAG_YRKESAKTIV, BrevDataByggerAvslagYrkesaktiv.class);
    }

    @Test
    final void hent_medDokumentTypeAnmodningUnntak_girBrevDataByggerAvslagOgAnmodningUnntak() {
        testHent(Produserbaredokumenter.ORIENTERING_ANMODNING_UNNTAK, BrevDataByggerAnmodningUnntak.class);
    }

    private void testHent(Produserbaredokumenter type, Class<? extends BrevDataBygger> forventetKlasse) {
        BrevDataBygger resultat = brevDataByggerVelger.hent(type, new BrevbestillingDto());
        assertThat(resultat).isInstanceOf(forventetKlasse);
    }
}
