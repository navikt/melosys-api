package no.nav.melosys.saksflyt.steg.ufm;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.RegisterkontrollService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class RegisterKontrollTest {

    @Mock
    private RegisterkontrollService registerkontrollService;

    private RegisterKontroll registerKontroll;

    @Before
    public void setup() {
        registerKontroll = new RegisterKontroll(registerkontrollService);
    }

    @Test
    public void utfør() throws Exception {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(new Behandling());
        prosessinstans.getBehandling().setId(1L);

        registerKontroll.utfør(prosessinstans);

        verify(registerkontrollService).utførKontrollerOgRegistrerFeil(anyLong());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_BESTEM_BEHANDLINGSMAATE);
    }
}