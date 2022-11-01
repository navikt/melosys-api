package no.nav.melosys.service.dokument.brev.bygger;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Bostedsland;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.service.LandvelgerService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevDataGrunnlag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class BrevDataByggerVideresendTest {

    @Mock
    LandvelgerService landvelgerService;

    @Mock
    UtenlandskMyndighetService utenlandskMyndighetService;

    @Mock
    BrevDataGrunnlag brevDataGrunnlag;

    private BrevDataByggerVideresend brevDataByggerVideresend;

    @BeforeEach
    public void setUp() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        when(brevDataGrunnlag.getBehandling()).thenReturn(behandling);

        brevDataByggerVideresend = new BrevDataByggerVideresend(landvelgerService, utenlandskMyndighetService, new BrevbestillingRequest());
    }

    @Test
    public void lag_medBostedSverigeOgTrygdemyndighetslandSverige_girBrevdata() {
        when(landvelgerService.hentBostedsland(eq(1L), any())).thenReturn(new Bostedsland(Landkoder.SE));

        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.navn = "Försäkringskassan";
        utenlandskMyndighet.gateadresse_1 = "Box 1164";
        utenlandskMyndighet.postnummer = "SE-621 22";
        utenlandskMyndighet.poststed = "Visby";
        utenlandskMyndighet.land = "Sverige";
        utenlandskMyndighet.landkode = Land_iso2.SE;
        when(utenlandskMyndighetService.hentUtenlandskMyndighet(eq(Land_iso2.SE))).thenReturn(utenlandskMyndighet);

        brevDataByggerVideresend.lag(brevDataGrunnlag, "Saksbehandler");
    }
}
