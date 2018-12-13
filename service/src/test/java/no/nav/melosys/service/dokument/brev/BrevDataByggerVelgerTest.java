package no.nav.melosys.service.dokument.brev;

import no.nav.melosys.domain.dokument.soeknad.SoeknadDokument;
import no.nav.melosys.service.RegisterOppslagSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.DokumentType;
import no.nav.melosys.service.kodeverk.KodeverkService;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;

@RunWith(MockitoJUnitRunner.class)
public class BrevDataByggerVelgerTest {

    private SoeknadDokument søknad;

    private BrevDataByggerVelger brevDataByggerVelger;

    @Before
    public void setUp() {
        KodeverkService kodeverkService = mock(KodeverkService.class);
        RegisterOppslagSystemService registerOppslagService = mock(RegisterOppslagSystemService.class);
        AvklartefaktaService avklartefaktaService = mock(AvklartefaktaService.class);

        brevDataByggerVelger = new BrevDataByggerVelger(avklartefaktaService, registerOppslagService, kodeverkService);

    }

    @Test
    public void testA1() throws Exception {
        testHent(DokumentType.ATTEST_A1, BrevDataByggerA1.class);
    }

    @Test
    public final void hentInnvilelsesYrksaktivGirA1Bygger() {
        testHent(DokumentType.INNVILGELSE_YRKESAKTIV, BrevDataByggerA1.class);
    }

    private final void testHent(DokumentType type, Class<? extends BrevDataBygger> forventetKlasse) {
        BrevDataBygger resultat = brevDataByggerVelger.hent(type);
        assertThat(resultat).isInstanceOf(forventetKlasse);
    }

    @Test
    public void testMangelbrev() {
        BrevbestillingDto bestilling = new BrevbestillingDto();
        BrevDataBygger bygger = brevDataByggerVelger.hent(DokumentType.MELDING_MANGLENDE_OPPLYSNINGER, bestilling);
        assertThat(bygger).isInstanceOf(BrevDataByggerStandard.class);
    }

    @Test
    public void testForvaltningsmelding() {
        BrevbestillingDto bestilling = new BrevbestillingDto();
        BrevDataBygger bygger = brevDataByggerVelger.hent(DokumentType.MELDING_FORVENTET_SAKSBEHANDLINGSTID, bestilling);
        assertThat(bygger).isInstanceOf(BrevDataByggerStandard.class);
    }




}