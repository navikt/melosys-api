package no.nav.melosys.service.dokument.brev.bygger;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
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
    UtenlandskMyndighetService utenlandskMyndighetService;

    @Mock
    BrevDataGrunnlag brevDataGrunnlag;

    private BrevDataByggerVideresend brevDataByggerVideresend;

    @Before
    public void setUp() throws FunksjonellException, TekniskException {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        when(brevDataGrunnlag.getBehandling()).thenReturn(behandling);

        brevDataByggerVideresend = new BrevDataByggerVideresend(landvelgerService, utenlandskMyndighetService, new BrevbestillingDto());
    }

    @Test
    public void lag_medBostedSverigeOgTrygdemyndighetslandSverige_girBrevdata() throws FunksjonellException, TekniskException {
        when(landvelgerService.hentBostedsland(eq(1L), any())).thenReturn(Landkoder.SE);

        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.navn = "Försäkringskassan";
        utenlandskMyndighet.gateadresse = "Box 1164";
        utenlandskMyndighet.postnummer = "SE-621 22";
        utenlandskMyndighet.poststed = "Visby";
        utenlandskMyndighet.land = "Sverige";
        utenlandskMyndighet.landkode = Landkoder.SE;
        when(utenlandskMyndighetService.hentUtenlandskMyndighet(eq(Landkoder.SE))).thenReturn(utenlandskMyndighet);

        brevDataByggerVideresend.lag(brevDataGrunnlag, "Saksbehandler");
    }
}