package no.nav.melosys.service.vedtak;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import no.nav.melosys.service.ProsessinstansService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
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

@RunWith(MockitoJUnitRunner.class)
public class VedtakServiceTest {

    @Mock
    private BehandlingRepository behandlingRepository;

    @Mock
    private Binge binge;

    @Mock
    private ProsessinstansRepository prosessinstansRepo;

    @Mock
    private OppgaveService oppgaveService;

    private VedtakService vedtakService;

    private long behandlingID;
    private Behandling behandling;

    @Captor
    private ArgumentCaptor<Prosessinstans> prosessinstansArgumentCaptor;

    @Before
    public void setUp() {
        ProsessinstansService prosessinstansService = new ProsessinstansService(binge, prosessinstansRepo);
        vedtakService = new VedtakService(behandlingRepository, oppgaveService, prosessinstansService);
        SpringSubjectHandler.set(new TestSubjectHandler());

        behandlingID = 1L;
        behandling = new Behandling();

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-111");
        behandling.setFagsak(fagsak);

        when(behandlingRepository.findOne(behandlingID)).thenReturn(behandling);
    }

    @Test
    public void fattVedtak_fungerer() throws FunksjonellException, TekniskException {
        Oppgave oppgave = new Oppgave();
        oppgave.setOppgaveId("1");
        BehandlingsresultatType resultatType = BehandlingsresultatType.FASTSATT_LOVVALGSLAND;

        vedtakService.fattVedtak(behandlingID, resultatType);

        verify(behandlingRepository, times(1)).findOne(behandlingID);
        verify(prosessinstansRepo, times(1)).save(prosessinstansArgumentCaptor.capture());

        Prosessinstans pi = prosessinstansArgumentCaptor.getValue();
        assertThat(pi.getType()).isEqualTo(ProsessType.IVERKSETT_VEDTAK);
        assertThat(pi.getSteg()).isEqualTo(ProsessSteg.IV_VALIDERING);
        assertThat(BehandlingsresultatType.valueOf(pi.getData(ProsessDataKey.BEHANDLINGSRESULTATTYPE))).isEqualTo(resultatType);

        verify(binge, times(1)).leggTil(any());
        verify(oppgaveService, times(1)).ferdigstillOppgaveMedSaksnummer(any());
    }

    @Test
    public void anmodningOmUnntak_fungerer() throws FunksjonellException, TekniskException {
        vedtakService.anmodningOmUnntak(behandlingID);

        verify(behandlingRepository, times(1)).findOne(behandlingID);
        verify(behandlingRepository, times(1)).save(behandling);
        verify(prosessinstansRepo, times(1)).save(prosessinstansArgumentCaptor.capture());
        assertThat(prosessinstansArgumentCaptor.getValue().getType()).isEqualTo(ProsessType.ANMODNING_OM_UNNTAK);
        assertThat(prosessinstansArgumentCaptor.getValue().getSteg()).isEqualTo(ProsessSteg.AOU_VALIDERING);

        verify(binge, times(1)).leggTil(any());
        verify(oppgaveService, times(1)).leggTilbakeOppgaveMedSaksnummer(any());
    }

    @Test(expected = IkkeFunnetException.class)
    public void fattVedtak_behandlingIkkeFunnet() throws FunksjonellException, TekniskException {
        long behandlingID = 0L;
        vedtakService.fattVedtak(behandlingID, BehandlingsresultatType.FASTSATT_LOVVALGSLAND);
    }
}