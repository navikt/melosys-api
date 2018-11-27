package no.nav.melosys.service;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsstatus;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BehandlingServiceTest {

    @Mock
    private BehandlingRepository behandlingRepo;

    private BehandlingService behandlingService;

    @Before
    public void setUp() {
        behandlingService = new BehandlingService(behandlingRepo);
    }

    @Test
    public void oppdaterStatus() throws FunksjonellException {
        long behandlingID = 11L;
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        when(behandlingRepo.findOne(anyLong())).thenReturn(behandling);
        behandlingService.oppdaterStatus(behandlingID, Behandlingsstatus.AVVENT_DOK_PART);
    }

    @Test(expected = IkkeFunnetException.class)
    public void oppdaterStatus_behIkkeFunnet() throws FunksjonellException {
        long behandlingID = 11L;
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        when(behandlingRepo.findOne(anyLong())).thenReturn(null);
        behandlingService.oppdaterStatus(behandlingID, Behandlingsstatus.AVVENT_DOK_PART);
    }

    @Test(expected = FunksjonellException.class)
    public void oppdaterStatus_ugyldig() throws FunksjonellException {
        long behandlingID = 11L;
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        behandlingService.oppdaterStatus(behandlingID, Behandlingsstatus.AVSLUTTET);
    }

    @Test(expected = FunksjonellException.class)
    public void oppdaterStatus_feilForrigeStatus() throws FunksjonellException {
        long behandlingID = 11L;
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        when(behandlingRepo.findOne(anyLong())).thenReturn(behandling);
        behandlingService.oppdaterStatus(behandlingID, Behandlingsstatus.AVVENT_DOK_PART);
    }
}