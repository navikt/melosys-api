package no.nav.melosys.service;

import java.util.List;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Registerkontroll;
import no.nav.melosys.domain.kodeverk.begrunnelser.Kontroll_begrunnelser;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.RegisterkontrollRepository;
import no.nav.melosys.service.kontroll.ufm.UfmKontrollService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RegisterkontrollServiceTest {
    @Mock
    private RegisterkontrollRepository registerkontrollRepository;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;
    @Mock
    private UfmKontrollService ufmKontrollService;
    @Mock
    private BehandlingService behandlingService;
    @Captor
    private ArgumentCaptor<List<Registerkontroll>> registerkontrollerCaptor;

    private RegisterkontrollService registerkontrollService;

    @Before
    public void setUp() throws IkkeFunnetException, TekniskException {
        registerkontrollService = new RegisterkontrollService(registerkontrollRepository, behandlingsresultatService, ufmKontrollService, behandlingService);

        when(registerkontrollRepository.saveAll(anyCollection())).thenReturn(List.of());
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
        registerkontrollService.utførKontrollerOgRegistrerFeil(1L);

        verify(behandlingService).hentBehandling(anyLong());
        verify(ufmKontrollService).utførKontroller(any(Behandling.class));
        verify(registerkontrollRepository).saveAll(registerkontrollerCaptor.capture());

        assertThat(registerkontrollerCaptor.getValue()).extracting(Registerkontroll::getBegrunnelse)
            .containsExactlyInAnyOrder(
                Kontroll_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND,
                Kontroll_begrunnelser.MOTTAR_YTELSER
            );
    }

    @Test
    public void leggTilRegisterkontroller_medTreff_validerLagring() throws IkkeFunnetException {
        registerkontrollService.lagreRegisterkontroller(1L, List.of(
            Kontroll_begrunnelser.LOVVALGSLAND_NORGE,
            Kontroll_begrunnelser.MOTTAR_YTELSER
        ));

        verify(registerkontrollRepository).saveAll(registerkontrollerCaptor.capture());
        List<Registerkontroll> registerkontroller = registerkontrollerCaptor.getValue();

        assertThat(registerkontroller).hasSize(2);

        assertThat(registerkontroller).extracting(Registerkontroll::getBegrunnelse)
            .containsExactlyInAnyOrder(Kontroll_begrunnelser.LOVVALGSLAND_NORGE, Kontroll_begrunnelser.MOTTAR_YTELSER);

        assertThat(registerkontroller).extracting(Registerkontroll::getBehandlingsresultat)
            .extracting(Behandlingsresultat::getId)
            .containsExactlyInAnyOrder(1L, 1L);
    }

    private Behandlingsresultat lagBehandlingsresultat() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        behandlingsresultat.setId(1L);

        return behandlingsresultat;
    }
}