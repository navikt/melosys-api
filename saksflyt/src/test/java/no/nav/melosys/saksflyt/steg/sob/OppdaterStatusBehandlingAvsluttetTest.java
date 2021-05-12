package no.nav.melosys.saksflyt.steg.sob;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.service.sob.SobService;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OppdaterStatusBehandlingAvsluttetTest {

    @Test
    void utfør() {
        SobService sobService = mock(SobService.class);
        OppdaterStatusBehandlingAvsluttet oppdaterStatusBehandlingAvsluttet = new OppdaterStatusBehandlingAvsluttet(sobService);

        Behandling behandling = new Behandling();
        behandling.setId(123321L);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        oppdaterStatusBehandlingAvsluttet.utfør(prosessinstans);
        verify(sobService).sakOgBehandlingAvsluttet(eq(behandling.getId()));
    }
}
