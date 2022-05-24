package no.nav.melosys.saksflyt.steg.register;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.behandling.kontroll.BehandlingskontrollresultatService;
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
    private BehandlingskontrollresultatService behandlingskontrollresultatService;

    private RegisterKontroll registerKontroll;

    @BeforeEach
    public void setup() {
        registerKontroll = new RegisterKontroll(behandlingskontrollresultatService);
    }

    @Test
    public void utfør() throws Exception {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(new Behandling());
        prosessinstans.getBehandling().setId(1L);

        registerKontroll.utfør(prosessinstans);

        verify(behandlingskontrollresultatService).utførKontrollerOgRegistrerFeil(anyLong());
    }
}
