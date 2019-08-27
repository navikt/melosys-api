package no.nav.melosys.saksflyt.steg.ufm;

import com.google.common.collect.Lists;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
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
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class RegisterKontrollTest {

    @Mock
    private AvklartefaktaService avklartefaktaService;
    @Mock
    private RegisterkontrollService registerkontrollServiceService;
    @Mock
    private BehandlingService behandlingService;

    private RegisterKontroll registerKontroll;

    @Captor
    private ArgumentCaptor<String> captor;

    @Before
    public void setup() throws Exception {
        registerKontroll = new RegisterKontroll(avklartefaktaService, registerkontrollServiceService, behandlingService);

        when(registerkontrollServiceService.utførKontroller(any(Behandling.class)))
            .thenReturn(Lists.newArrayList(
                Unntak_periode_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND,
                Unntak_periode_begrunnelser.MOTTAR_YTELSER)
            );
    }

    @Test
    public void utfør() throws Exception {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(new Behandling());
        prosessinstans.getBehandling().setId(1L);

        when(behandlingService.hentBehandling(anyLong())).thenReturn(new Behandling());
        registerKontroll.utfør(prosessinstans);

        verify(registerkontrollServiceService).utførKontroller(any(Behandling.class));
        verify(avklartefaktaService).leggTilAvklarteFakta(anyLong(), eq(Avklartefaktatyper.VURDERING_UNNTAK_PERIODE), anyString(), any(), eq("TRUE"));
        verify(avklartefaktaService, times(2)).leggTilRegistrering(anyLong(), eq(Avklartefaktatyper.VURDERING_UNNTAK_PERIODE), captor.capture());

        assertThat(captor.getAllValues()).containsExactly(
            Unntak_periode_begrunnelser.TREDJELANDSBORGER_IKKE_AVTALELAND.getKode(),
            Unntak_periode_begrunnelser.MOTTAR_YTELSER.getKode()
        );

        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_BESTEM_BEHANDLINGSMAATE);
    }
}