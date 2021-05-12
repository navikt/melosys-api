package no.nav.melosys.service;

import java.util.List;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Kontrollresultat;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.repository.KontrollresultatRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.kontroll.KontrollresultatService;
import no.nav.melosys.service.kontroll.ufm.UfmKontrollService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class KontrollresultatServiceTest {
    @Mock
    private KontrollresultatRepository kontrollresultatRepository;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private UfmKontrollService ufmKontrollService;
    @Mock
    private BehandlingService behandlingService;
    @Captor
    private ArgumentCaptor<List<Kontrollresultat>> kontrollresultaterCaptor;

    private KontrollresultatService kontrollresultatService;

    @BeforeEach
    public void setUp() {
        kontrollresultatService = new KontrollresultatService(kontrollresultatRepository, behandlingsresultatService, ufmKontrollService, behandlingService);

        when(kontrollresultatRepository.saveAll(anyCollection())).thenReturn(List.of());
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(lagBehandlingsresultat());
        when(ufmKontrollService.utførKontroller(any(Behandling.class)))
            .thenReturn(Lists.newArrayList(
                Kontroll_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND,
                Kontroll_begrunnelser.MOTTAR_YTELSER)
            );
    }

    @Test
    public void utførKontrollerOgRegistrerFeil() throws Exception {
        when(behandlingService.hentBehandling(anyLong())).thenReturn(new Behandling());
        kontrollresultatService.utførKontrollerOgRegistrerFeil(1L);

        verify(behandlingService).hentBehandling(anyLong());
        verify(ufmKontrollService).utførKontroller(any(Behandling.class));
        verify(kontrollresultatRepository).deleteByBehandlingsresultat(any(Behandlingsresultat.class));
        verify(kontrollresultatRepository).saveAll(kontrollresultaterCaptor.capture());

        List<Kontrollresultat> kontrollresultater = kontrollresultaterCaptor.getValue();

        assertThat(kontrollresultater).hasSize(2);

        assertThat(kontrollresultater).extracting(Kontrollresultat::getBegrunnelse)
            .containsExactlyInAnyOrder(
                Kontroll_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND,
                Kontroll_begrunnelser.MOTTAR_YTELSER
            );

        assertThat(kontrollresultater).extracting(Kontrollresultat::getBehandlingsresultat)
            .extracting(Behandlingsresultat::getId)
            .containsExactlyInAnyOrder(1L, 1L);
    }

    private Behandlingsresultat lagBehandlingsresultat() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);

        return behandlingsresultat;
    }
}
