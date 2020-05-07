package no.nav.melosys.service.dokument.brev.bygger;

import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.integrasjon.joark.JoarkService;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.SaksopplysningerService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevDataByggerVelger;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.unntak.AnmodningsperiodeService;
import no.nav.melosys.service.utpeking.UtpekingService;
import no.nav.melosys.service.vilkaar.VilkaarsresultatService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class BrevDataByggerVelgerTest {

    private BrevDataByggerVelger brevDataByggerVelger;

    @Before
    public void setUp() {
        AnmodningsperiodeService anmodningsperiodeService = mock(AnmodningsperiodeService.class);
        AvklartefaktaService avklartefaktaService = mock(AvklartefaktaService.class);
        JoarkService joarkService = mock(JoarkService.class);
        LandvelgerService landvelgerService = mock(LandvelgerService.class);
        LovvalgsperiodeService lovvalgsperiodeService = mock(LovvalgsperiodeService.class);
        SaksopplysningerService saksopplysningerService = mock(SaksopplysningerService.class);
        UtenlandskMyndighetService utenlandskMyndighetService = mock(UtenlandskMyndighetService.class);
        UtpekingService utpekingService = mock(UtpekingService.class);
        VilkaarsresultatRepository vilkaarsresultatRepository = mock(VilkaarsresultatRepository.class);
        VilkaarsresultatService vilkaarsresultatService = mock(VilkaarsresultatService.class);

        brevDataByggerVelger = new BrevDataByggerVelger(anmodningsperiodeService, avklartefaktaService, joarkService,
            landvelgerService, lovvalgsperiodeService, saksopplysningerService,
            utenlandskMyndighetService, utpekingService, vilkaarsresultatRepository, vilkaarsresultatService);
    }

    @Test
    public void hent_medAttestA1_girVedleggBygger() {
        testHent(Produserbaredokumenter.ATTEST_A1, BrevDataByggerVedlegg.class);
    }

    @Test
    public void hent_medSEDA001_girVedleggBygger() {
        testHent(Produserbaredokumenter.ANMODNING_UNNTAK, BrevDataByggerVedlegg.class);
    }

    @Test
    public final void hent_InnvilgelsesYrksaktiv_girInnvilgelseBygger() {
        testHent(Produserbaredokumenter.INNVILGELSE_YRKESAKTIV, BrevDataByggerInnvilgelse.class);
    }

    @Test
    public final void hent_medDokumentTypeINNVILGELSE_YRKESAKTIV_FLERE_LAND_girBrevDataByggerInnvilgelseFlereLand() {
        testHent(Produserbaredokumenter.INNVILGELSE_YRKESAKTIV_FLERE_LAND, BrevDataByggerInnvilgelseFlereLand.class);
    }

    @Test
    public final void hent_InnvilgelsesArbeidsgiver_girInnvilgelseBygger() {
        testHent(Produserbaredokumenter.INNVILGELSE_ARBEIDSGIVER, BrevDataByggerInnvilgelse.class);
    }

    @Test
    public final void hent_Avslag_girBrevDataByggerAvslagOgAnmodningUnntak() {
        testHent(Produserbaredokumenter.AVSLAG_YRKESAKTIV, BrevDataByggerAvslagYrkesaktiv.class);
    }

    @Test
    public final void hent_medDokumentTypeAnmodningUnntak_girBrevDataByggerAvslagOgAnmodningUnntak() {
        testHent(Produserbaredokumenter.ORIENTERING_ANMODNING_UNNTAK, BrevDataByggerAnmodningUnntak.class);
    }

    @Test
    public final void hent_medDokumentTypeMELDING_MANGLENDE_OPPLYSNINGER_girBrevDataByggerForsendelseMottattDato() {
        testHent(Produserbaredokumenter.MELDING_MANGLENDE_OPPLYSNINGER, BrevDataByggerMedMottattDato.class);
    }

    @Test
    public final void hent_medDokumentTypeMELDING_FORVENTET_SAKSBEHANDLINGSTID_girBrevDataByggerForsendelseMottattDato() {
        testHent(Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID, BrevDataByggerMedMottattDato.class);
    }

    @Test
    public void testMangelbrev() {
        BrevbestillingDto bestilling = new BrevbestillingDto();
        BrevDataBygger bygger = brevDataByggerVelger.hent(Produserbaredokumenter.MELDING_MANGLENDE_OPPLYSNINGER, bestilling);
        assertThat(bygger).isInstanceOf(BrevDataByggerMedMottattDato.class);
    }

    @Test
    public void testForvaltningsmelding() {
        BrevbestillingDto bestilling = new BrevbestillingDto();
        BrevDataBygger bygger = brevDataByggerVelger.hent(Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID, bestilling);
        assertThat(bygger).isInstanceOf(BrevDataByggerMedMottattDato.class);
    }

    private void testHent(Produserbaredokumenter type, Class<? extends BrevDataBygger> forventetKlasse) {
        BrevDataBygger resultat = brevDataByggerVelger.hent(type);
        assertThat(resultat).isInstanceOf(forventetKlasse);
    }
}