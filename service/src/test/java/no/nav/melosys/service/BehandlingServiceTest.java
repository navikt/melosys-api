package no.nav.melosys.service;

import java.util.Optional;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.ProsessinstansRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        when(prosessinstansRepository.findByTypeAndStegIsNotNullAndStegIsNotAndBehandling_Id(ProsessType.OPPFRISKNING, ProsessSteg.FEILET_MASKINELT, 111L)).thenReturn(Optional.empty());
        assertThat(behandlingService.harAktivOppfrisking(111L)).isFalse();

        Prosessinstans process = mock(Prosessinstans.class);
        when(prosessinstansRepository.findByTypeAndStegIsNotNullAndStegIsNotAndBehandling_Id(ProsessType.OPPFRISKNING, ProsessSteg.FEILET_MASKINELT, 111L)).thenReturn(Optional.of(process));
        assertThat(behandlingService.harAktivOppfrisking(111L)).isTrue();
    }
}