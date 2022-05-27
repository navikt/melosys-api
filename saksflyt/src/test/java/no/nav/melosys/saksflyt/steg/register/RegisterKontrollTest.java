package no.nav.melosys.saksflyt.steg.register;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.kontroll.ufm.UfmKontrollService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class RegisterKontrollTest {

    @Mock
    private UfmKontrollService ufmKontrollService;

    private RegisterKontroll registerKontroll;

    @BeforeEach
    public void setup() {
        registerKontroll = new RegisterKontroll(ufmKontrollService);
    }

    @Test
    public void utfør() throws Exception {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(new Behandling());
        prosessinstans.getBehandling().setId(1L);

        registerKontroll.utfør(prosessinstans);

        verify(ufmKontrollService).utførKontrollerOgRegistrerFeil(anyLong());
    }
}
