package no.nav.melosys.saksflyt.agent.jfr;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsstatus;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.repository.BehandlingRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterBehandlingsstatusTest {

    private BehandlingRepository behandlingRepository;

    private OppdaterBehandlingsstatus agent;

    @Before
    public void setUp() throws Exception {
        behandlingRepository = mock(BehandlingRepository.class);
        agent = new OppdaterBehandlingsstatus(behandlingRepository);
    }

    @Test
    public void behandlingAvventerDokumentasjon() {
        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.AVVENT_DOK_PART);
        p.setBehandling(behandling);

        agent.utførSteg(p);

        assertThat(behandling.getStatus()).isEqualTo(Behandlingsstatus.VURDER_DOKUMENT);
        verify(behandlingRepository, times(1)).save(any(Behandling.class));
        assertThat(p.getSteg()).isNull();
    }

    @Test
    public void behandlingAvventerIkkeDokumentasjon() {
        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        p.setBehandling(behandling);

        agent.utførSteg(p);

        assertThat(behandling.getStatus()).isEqualTo(Behandlingsstatus.UNDER_BEHANDLING);
        verify(behandlingRepository, times(0)).save(any(Behandling.class));
        assertThat(p.getSteg()).isNull();
    }
}
