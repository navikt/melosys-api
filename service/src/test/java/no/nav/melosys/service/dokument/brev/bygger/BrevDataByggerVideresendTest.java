package no.nav.melosys.service.dokument.brev.bygger;

import java.util.Collections;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.dokument.LandvelgerService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BrevDataByggerVideresendTest {

    @Mock
    LandvelgerService landvelgerService;

    @Mock
    BrevDataGrunnlag brevDataGrunnlag;

    private BrevDataByggerVideresend brevDataByggerVideresend;

    @Before
    public void setUp() throws FunksjonellException, TekniskException {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        when(brevDataGrunnlag.getBehandling()).thenReturn(behandling);

        brevDataByggerVideresend = new BrevDataByggerVideresend(landvelgerService, new BrevbestillingDto());
    }

    @Test
    public void lag_medBostedSverigeOgTrygdemyndighetslandSverige_girBrevdata() throws FunksjonellException, TekniskException {
        when(landvelgerService.hentBostedsland(eq(1L), any())).thenReturn(Landkoder.SE);
        when(landvelgerService.hentUtenlandskTrygdemyndighetsland(eq(1L))).thenReturn(Collections.singleton(Landkoder.SE));

        brevDataByggerVideresend.lag(brevDataGrunnlag, "Saksbehandler");
    }
}