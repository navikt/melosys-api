package no.nav.melosys.service;

import no.nav.melosys.domain.*;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.SubjectHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;

@RunWith(MockitoJUnitRunner.class)
public class ProsessinstansServiceTest {

    @Mock
    private Binge binge;

    @Mock
    private ProsessinstansRepository prosessinstansRepo;

    @Captor
    private ArgumentCaptor<Prosessinstans> piCaptor;

    private ProsessinstansService service;

    @Before
    public void setUp() {
        service = new ProsessinstansService(binge, prosessinstansRepo);
    }

    @Test
    public void lagreProsessinstans_medSaksbehandler() {
        Prosessinstans prosessinstans = mock(Prosessinstans.class);
        String saksbehandler = "Z123456";
        service.lagreProsessinstans(prosessinstans, saksbehandler);

        verify(prosessinstans, times(1)).setEndretDato(any());
        verify(prosessinstans, times(1)).setRegistrertDato(any());
        verify(prosessinstans, times(1)).setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);
    }

    @Test
    public void lagreProsessinstans_utenSaksbehandler_henterFraSubjectHandler() {
        String saksbehandler = "Z123456";
        SubjectHandler subjectHandler = mock(SpringSubjectHandler.class);
        SubjectHandler.set(subjectHandler);
        when(subjectHandler.getUserID()).thenReturn(saksbehandler);

        Prosessinstans prosessinstans = mock(Prosessinstans.class);
        service.lagreProsessinstans(prosessinstans);

        verify(prosessinstans, times(1)).setData(ProsessDataKey.SAKSBEHANDLER, saksbehandler);
    }

    @Test
    public void opprettProsessinstansAnmodningOmUnntak() {
        Behandling behandling = new Behandling();
        service.opprettProsessinstansAnmodningOmUnntak(behandling);

        verify(prosessinstansRepo, times(1)).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.ANMODNING_OM_UNNTAK);
        assertThat(lagretInstans.getSteg()).isEqualTo(ProsessSteg.AOU_VALIDERING);
        assertThat(lagretInstans.getBehandling()).isEqualTo(behandling);
    }

    @Test
    public void opprettProsessinstansIverksettVedtak_medBehandlingOgBehandlingsresultat() {
        Behandling behandling = new Behandling();
        BehandlingsresultatType resultatType = BehandlingsresultatType.FASTSATT_LOVVALGSLAND;
        service.opprettProsessinstansIverksettVedtak(behandling, resultatType);

        verify(prosessinstansRepo, times(1)).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.IVERKSETT_VEDTAK);
        assertThat(lagretInstans.getSteg()).isEqualTo(ProsessSteg.IV_VALIDERING);
        assertThat(lagretInstans.getBehandling()).isEqualTo(behandling);
        assertThat(BehandlingsresultatType.valueOf(lagretInstans.getData(ProsessDataKey.BEHANDLINGSRESULTATTYPE))).isEqualTo(resultatType);
    }

    @Test
    public void opprettProsessinstansHenleggeSak() {
        String saksbehandler = "Z123456";
        SubjectHandler subjectHandler = mock(SpringSubjectHandler.class);
        SubjectHandler.set(subjectHandler);
        when(subjectHandler.getUserID()).thenReturn(saksbehandler);

        Behandling behandling = new Behandling();
        service.opprettProsessinstansOppdaterBehandlingsresultatHenleggSak(behandling, "ANNET", "");

        verify(prosessinstansRepo, times(1)).save(piCaptor.capture());

        Prosessinstans lagretInstans = piCaptor.getValue();
        assertThat(lagretInstans.getType()).isEqualTo(ProsessType.HENLEGG_SAK);
        assertThat(lagretInstans.getSteg()).isEqualTo(ProsessSteg.OPPDATER_RESULTAT_HENLEGG_SAK);
        assertThat(lagretInstans.getBehandling()).isEqualTo(behandling);
    }
}
