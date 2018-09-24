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
    public void sjekkStatusBehandling() {
        when(prosessinstansRepository.findByStegIsNotNullAndBehandling_Id(anyLong())).thenReturn(Optional.empty());
        assertThat(behandlingService.aktivProsessinstansEksistererFor(anyLong())).isFalse();

        Prosessinstans process = mock(Prosessinstans.class);
        when(prosessinstansRepository.findByStegIsNotNullAndBehandling_Id(anyLong())).thenReturn(Optional.of(process));
        assertThat(behandlingService.aktivProsessinstansEksistererFor(anyLong())).isTrue();
    }
}