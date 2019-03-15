package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.kodeverk.Produserbaredokumenter;
import no.nav.melosys.integrasjon.joark.JoarkService;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.RegisterOppslagSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.brev.bygger.*;
import no.nav.melosys.service.kodeverk.KodeverkService;
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
        KodeverkService kodeverkService = mock(KodeverkService.class);
        RegisterOppslagSystemService registerOppslagService = mock(RegisterOppslagSystemService.class);
        AvklartefaktaService avklartefaktaService = mock(AvklartefaktaService.class);
        LovvalgsperiodeService lovvalgsperiodeService = mock(LovvalgsperiodeService.class);
        VilkaarsresultatRepository vilkaarsresultatRepository = mock(VilkaarsresultatRepository.class);
        UtenlandskMyndighetRepository utenlandskMyndighetRepository = mock(UtenlandskMyndighetRepository.class);
        JoarkService joarkService = mock(JoarkService.class);

        brevDataByggerVelger = new BrevDataByggerVelger(avklartefaktaService, registerOppslagService, kodeverkService, lovvalgsperiodeService, utenlandskMyndighetRepository, vilkaarsresultatRepository, joarkService);
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
    public final void hent_Avslag_girBrevDataByggerAvslagOgAnmodningUnntak() {
        testHent(Produserbaredokumenter.AVSLAG_YRKESAKTIV, BrevDataByggerAnmodningUnntakOgAvslag.class);
    }

    @Test
    public final void hent_medDokumentTypeAnmodningUnntak_girBrevDataByggerAvslagOgAnmodningUnntak() {
        testHent(Produserbaredokumenter.ORIENTERING_ANMODNING_UNNTAK, BrevDataByggerAnmodningUnntakOgAvslag.class);
    }

    @Test
    public void testMangelbrev() {
        BrevbestillingDto bestilling = new BrevbestillingDto();
        BrevDataBygger bygger = brevDataByggerVelger.hent(Produserbaredokumenter.MELDING_MANGLENDE_OPPLYSNINGER, bestilling);
        assertThat(bygger).isInstanceOf(BrevDataByggerStandard.class);
    }

    @Test
    public void testForvaltningsmelding() {
        BrevbestillingDto bestilling = new BrevbestillingDto();
        BrevDataBygger bygger = brevDataByggerVelger.hent(Produserbaredokumenter.MELDING_FORVENTET_SAKSBEHANDLINGSTID, bestilling);
        assertThat(bygger).isInstanceOf(BrevDataByggerStandard.class);
    }

    private void testHent(Produserbaredokumenter type, Class<? extends BrevDataBygger> forventetKlasse) {
        BrevDataBygger resultat = brevDataByggerVelger.hent(type);
        assertThat(resultat).isInstanceOf(forventetKlasse);
    }
}