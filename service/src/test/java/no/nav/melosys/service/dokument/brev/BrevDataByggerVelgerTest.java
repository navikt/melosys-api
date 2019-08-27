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
        AvklartefaktaService avklartefaktaService = mock(AvklartefaktaService.class);
        AnmodningsperiodeService anmodningsperiodeService = mock(AnmodningsperiodeService.class);
        LovvalgsperiodeService lovvalgsperiodeService = mock(LovvalgsperiodeService.class);
        VilkaarsresultatRepository vilkaarsresultatRepository = mock(VilkaarsresultatRepository.class);
        UtenlandskMyndighetRepository utenlandskMyndighetRepository = mock(UtenlandskMyndighetRepository.class);
        JoarkService joarkService = mock(JoarkService.class);

        brevDataByggerVelger = new BrevDataByggerVelger(anmodningsperiodeService, avklartefaktaService,
            lovvalgsperiodeService, utenlandskMyndighetRepository, vilkaarsresultatRepository, joarkService);
    }

    @Test
    public void hent_medAttestA1_girVedleggBygger() throws TekniskException {
        testHent(Produserbaredokumenter.ATTEST_A1, BrevDataByggerVedlegg.class);
    }

    @Test
    public void hent_medSEDA001_girVedleggBygger() throws TekniskException {
        testHent(Produserbaredokumenter.ANMODNING_UNNTAK, BrevDataByggerVedlegg.class);
    }

    @Test
    public final void hent_InnvilgelsesYrksaktiv_girInnvilgelseBygger() throws TekniskException {
        testHent(Produserbaredokumenter.INNVILGELSE_YRKESAKTIV, BrevDataByggerInnvilgelse.class);
    }

    @Test
    public final void hent_medDokumentTypeINNVILGELSE_YRKESAKTIV_FLERE_LAND_girBrevDataByggerInnvilgelseFlereLand() throws TekniskException {
        testHent(Produserbaredokumenter.INNVILGELSE_YRKESAKTIV_FLERE_LAND, BrevDataByggerInnvilgelseFlereLand.class);
    }

    @Test
    public final void hent_InnvilgelsesArbeidsgiver_girInnvilgelseBygger() throws TekniskException {
        testHent(Produserbaredokumenter.INNVILGELSE_ARBEIDSGIVER, BrevDataByggerInnvilgelse.class);
    }

    @Test
    public final void hent_Avslag_girBrevDataByggerAvslagOgAnmodningUnntak() throws TekniskException {
        testHent(Produserbaredokumenter.AVSLAG_YRKESAKTIV, BrevDataByggerAnmodningUnntakOgAvslag.class);
    }

    @Test
    public final void hent_medDokumentTypeAnmodningUnntak_girBrevDataByggerAvslagOgAnmodningUnntak() throws TekniskException {
        testHent(Produserbaredokumenter.ORIENTERING_ANMODNING_UNNTAK, BrevDataByggerAnmodningUnntakOgAvslag.class);
    }

    @Test
    public final void hent_medDokumentTypeMELDING_MANGLENDE_OPPLYSNINGER_girBrevDataByggerForsendelseMottattDato() throws TekniskException {
        testHent(Produserbaredokumenter.MELDING_MANGLENDE_OPPLYSNINGER, BrevDataByggerMedMottattDato.class);
    }

    @Test
    public final void hent_medDokumentTypeMELDING_FORVENTET_SAKSBEHANDLINGSTID_girBrevDataByggerForsendelseMottattDato() throws TekniskException {
        testHent(Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID, BrevDataByggerMedMottattDato.class);
    }

    @Test
    public void testMangelbrev() throws TekniskException {
        BrevbestillingDto bestilling = new BrevbestillingDto();
        Brevressurser brevressurser = mock(Brevressurser.class);
        BrevDataBygger bygger = brevDataByggerVelger.hent(Produserbaredokumenter.MELDING_MANGLENDE_OPPLYSNINGER, brevressurser, bestilling);
        assertThat(bygger).isInstanceOf(BrevDataByggerMedMottattDato.class);
    }

    @Test
    public void testForvaltningsmelding() throws TekniskException {
        BrevbestillingDto bestilling = new BrevbestillingDto();
        Brevressurser brevressurser = mock(Brevressurser.class);
        BrevDataBygger bygger = brevDataByggerVelger.hent(Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID, brevressurser, bestilling);
        assertThat(bygger).isInstanceOf(BrevDataByggerMedMottattDato.class);
    }

    private void testHent(Produserbaredokumenter type, Class<? extends BrevDataBygger> forventetKlasse) throws TekniskException {
        BrevDataBygger resultat = brevDataByggerVelger.hent(type, mock(Brevressurser.class));
        assertThat(resultat).isInstanceOf(forventetKlasse);
    }
}