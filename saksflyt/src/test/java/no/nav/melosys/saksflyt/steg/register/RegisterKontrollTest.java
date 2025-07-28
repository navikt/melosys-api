package no.nav.melosys.saksflyt.steg.register;

import no.nav.melosys.domain.BehandlingTestFactory;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.kontroll.feature.ufm.UfmKontrollService;
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
        prosessinstans.setBehandling(BehandlingTestFactory.builderWithDefaults().build());
        prosessinstans.getBehandling().setId(1L);

        registerKontroll.utfør(prosessinstans);

        verify(ufmKontrollService).utførKontrollerOgRegistrerFeil(anyLong());
    }
}
