package no.nav.melosys.service.dokument.sed;

import no.nav.melosys.eux.model.SedType;
import no.nav.melosys.repository.UtenlandskMyndighetRepository;
import no.nav.melosys.repository.VilkaarsresultatRepository;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.RegisterOppslagSystemService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.dokument.sed.bygger.A009DataBygger;
import no.nav.melosys.service.dokument.sed.bygger.AbstraktSedDataBygger;
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
    @Mock
    private UtenlandskMyndighetRepository utenlandskMyndighetRepository;
    @Mock
    private VilkaarsresultatRepository vilkaarsresultatRepository;

    @InjectMocks
    private SedDataByggerVelger sedDataByggerVelger;

    @Test
    public void hentDatabygger_forventA009DataBygger() {
        AbstraktSedDataBygger bygger = sedDataByggerVelger.hent(SedType.A009);
        assertThat(bygger, is(notNullValue()));
        assertThat(bygger, is(instanceOf(A009DataBygger.class)));
    }

    @Test(expected = RuntimeException.class)
    public void hentDatabygger_ikkeImplementert_forventException() {
        sedDataByggerVelger.hent(SedType.A008);
    }
}