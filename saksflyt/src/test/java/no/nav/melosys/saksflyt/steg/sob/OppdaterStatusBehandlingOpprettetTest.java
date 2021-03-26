package no.nav.melosys.saksflyt.steg.sob;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.service.sob.SobService;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OppdaterStatusBehandlingOpprettetTest {
    @Test
    void utfør() throws FunksjonellException, TekniskException {
        SobService sobService = mock(SobService.class);
        OppdaterStatusBehandlingOpprettet oppdaterStatusBehandlingAvsluttet = new OppdaterStatusBehandlingOpprettet(sobService);

        Behandling behandling = new Behandling();
        behandling.setId(123321L);
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);

        oppdaterStatusBehandlingAvsluttet.utfør(prosessinstans);
        verify(sobService).sakOgBehandlingOpprettet(eq(behandling.getId()));
    }
}