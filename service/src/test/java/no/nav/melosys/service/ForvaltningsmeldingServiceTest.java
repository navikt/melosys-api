package no.nav.melosys.service;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.exception.*;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ForvaltningsmeldingServiceTest {
    
    private ForvaltningsmeldingService forvaltningsmeldingService;

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private ProsessinstansService prosessinstansService;
    
    @Before
    public void setup() {
        forvaltningsmeldingService = new ForvaltningsmeldingService(prosessinstansService, behandlingService);
    }
    
    @Test
    public void sendForvaltningsmelding_fungerer() throws MelosysException {
        long behandlingID = 1L;
        Behandling behandling = new Behandling();
        when(behandlingService.hentBehandling(behandlingID)).thenReturn(behandling);

        forvaltningsmeldingService.sendForvaltningsmelding(behandlingID);

        verify(behandlingService).hentBehandling(eq(behandlingID));
        verify(prosessinstansService).opprettProsessinstansForvaltningsmeldingSend(eq(behandling));
        
    }
}