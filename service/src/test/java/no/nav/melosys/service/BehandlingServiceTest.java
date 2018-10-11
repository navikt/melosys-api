package no.nav.melosys.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import no.nav.melosys.domain.*;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.ProsessinstansRepository;

@RunWith(MockitoJUnitRunner.class)
public class BehandlingServiceTest {

    private ProsessinstansRepository prosessinstansRepository;

    private BehandlingRepository behandlingRepo;

    private BehandlingService behandlingService;

    @Before
    public void setUp() {
        behandlingRepo = mock(BehandlingRepository.class);
        prosessinstansRepository = mock(ProsessinstansRepository.class);

        behandlingService = new BehandlingService(prosessinstansRepository, behandlingRepo);
    }

    @Test
    public void sjekkStatusBehandlingForOppfrisking() {
        when(prosessinstansRepository.findByStegIsNotNullAndTypeAndBehandling_Id(ProsessType.OPPFRISKNING, 111L)).thenReturn(Optional.empty());
        assertThat(behandlingService.harAktivOppfrisking(111L)).isFalse();

        Prosessinstans process = mock(Prosessinstans.class);
        when(prosessinstansRepository.findByStegIsNotNullAndTypeAndBehandling_Id(ProsessType.OPPFRISKNING, 111L)).thenReturn(Optional.of(process));
        assertThat(behandlingService.harAktivOppfrisking(111L)).isTrue();
    }
}