package no.nav.melosys.service.dokument.sed;

import no.nav.melosys.domain.kodeverk.LovvalgsBestemmelser_883_2004;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.RegisterOppslagSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.sed.bygger.SedDataBygger;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;


@RunWith(MockitoJUnitRunner.class)
public class SedDataByggerVelgerTest {

    @Mock
    private AvklartefaktaService avklartefaktaService;
    @Mock
    private RegisterOppslagSystemService registerOppslagService;
    @Mock
    private KodeverkService kodeverkService;
    @Mock
    private LovvalgsperiodeService lovvalgsperiodeService;

    @InjectMocks
    private SedDataByggerVelger sedDataByggerVelger;

    @Test
    public void hentDatabygger_forventA009DataBygger() {
        SedDataBygger bygger = sedDataByggerVelger.hent(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_2);
        assertThat(bygger, is(notNullValue()));
        assertThat(bygger, is(instanceOf(SedDataBygger.class)));
    }

    @Test(expected = UnsupportedOperationException.class)
    public void hentDatabygger_ikkeImplementert_forventException() {
        sedDataByggerVelger.hent(LovvalgsBestemmelser_883_2004.FO_883_2004_ART11_1);
    }
}