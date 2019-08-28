package no.nav.melosys.saksflyt.felles;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Unntak_periode_begrunnelser;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.avklartefakta.AvklartefaktaService;
import no.nav.melosys.service.unntaksperiode.kontroll.RegisterkontrollService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RegisterKontrollFellesTest {

    @Mock
    private AvklartefaktaService avklartefaktaService;
    @Mock
    private RegisterkontrollService registerkontrollService;
    @Mock
    private BehandlingService behandlingService;

    private RegisterKontrollFelles registerKontrollFelles;

    @Captor
    private ArgumentCaptor<String> captor;

    @Before
    public void setup() throws Exception {
        registerKontrollFelles = new RegisterKontrollFelles(behandlingService, registerkontrollService, avklartefaktaService);
        when(registerkontrollService.utførKontroller(any(Behandling.class)))
            .thenReturn(Lists.newArrayList(
                Unntak_periode_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND,
                Unntak_periode_begrunnelser.MOTTAR_YTELSER)
            );
    }

    @Test
    public void utfør() throws Exception {
        when(behandlingService.hentBehandling(anyLong())).thenReturn(new Behandling());
        registerKontrollFelles.utførKontrollerOgRegistrerFeil(1L);

        verify(behandlingService).hentBehandling(anyLong());
        verify(registerkontrollService).utførKontroller(any(Behandling.class));
        verify(avklartefaktaService).leggTilAvklarteFakta(anyLong(), eq(Avklartefaktatyper.VURDERING_UNNTAK_PERIODE), anyString(), any(), eq("TRUE"));
        verify(avklartefaktaService, times(2)).leggTilRegistrering(anyLong(), eq(Avklartefaktatyper.VURDERING_UNNTAK_PERIODE), captor.capture());

        assertThat(captor.getAllValues()).containsExactly(
            Unntak_periode_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND.getKode(),
            Unntak_periode_begrunnelser.MOTTAR_YTELSER.getKode()
        );
    }
}