package no.nav.melosys.saksflyt.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingStatus;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.api.Binge;

//@RunWith(MockitoJUnitRunner.class)
public class InitBingeTest {

    /* FIXME
    @Mock
    private Binge binge;

    @Mock
    private BehandlingRepository behandlingRepo;

    private InitBinge initBinge;

    @Before
    public void setUp() {
        initBinge = new InitBinge(binge, behandlingRepo);
    }

    @Test
    public void afterPropertiesSet() throws Exception {
        List<Behandling> testBehandlinger = new ArrayList<>();

        Behandling b1 = new Behandling();
        b1.setStatus(BehandlingStatus.OPPRETTET);
        Behandling b2 = new Behandling();
        b2.setStatus(BehandlingStatus.UNDER_BEHANDLING);

        testBehandlinger.add(b1);
        testBehandlinger.add(b2);

        when(behandlingRepo.findByStatusNot(BehandlingStatus.AVSLUTTET)).thenReturn(testBehandlinger);
        when(binge.leggTil(any(Behandling.class))).thenReturn(true);

        initBinge.afterPropertiesSet();

        verify(binge, times(testBehandlinger.size())).leggTil(any(Behandling.class));
    }

*/
}