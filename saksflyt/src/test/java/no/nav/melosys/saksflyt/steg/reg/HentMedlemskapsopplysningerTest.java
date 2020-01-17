package no.nav.melosys.saksflyt.steg.reg;

import java.time.LocalDate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.service.SaksopplysningerService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.HENT_SOB_SAKER;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class HentMedlemskapsopplysningerTest {

    @Mock
    private SaksopplysningerService saksopplysningerService;

    private HentMedlemskapsopplysninger hentMedlemskapsopplysninger;

    @Before
    public void setUp() {
        hentMedlemskapsopplysninger = new HentMedlemskapsopplysninger(saksopplysningerService);
    }

    @Test
    public void utfoerSteg() throws MelosysException {
        final long behandlingID = 222L;
        Behandling behandling = new Behandling();
        behandling.setId(behandlingID);

        Prosessinstans p = new Prosessinstans();
        p.setBehandling(behandling);
        Periode periode = new Periode(LocalDate.now(), LocalDate.now().plusYears(1));
        p.setData(ProsessDataKey.SØKNADSPERIODE, periode);

        hentMedlemskapsopplysninger.utførSteg(p);

        verify(saksopplysningerService, times(1))
            .hentSaksopplysningMedl(eq(behandlingID), eq(periode.getFom()), eq(periode.getTom()));
        assertThat(p.getSteg()).isEqualTo(HENT_SOB_SAKER);
    }

}
