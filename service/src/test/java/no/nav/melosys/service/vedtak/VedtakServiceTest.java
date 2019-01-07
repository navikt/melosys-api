package no.nav.melosys.service.vedtak;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
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

    @Captor
    private ArgumentCaptor<Prosessinstans> prosessinstansArgumentCaptor;

    @Before
    public void setUp() {
        vedtakService = new VedtakService(behandlingRepository, binge, prosessinstansRepo, oppgaveService);
        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    public void fattVedtak() throws FunksjonellException, TekniskException {
        Long behandlingID = 1L;
        Behandling behandling = new Behandling();

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-111");
        behandling.setFagsak(fagsak);

        when(behandlingRepository.findOne(behandlingID)).thenReturn(behandling);
        Oppgave oppgave = new Oppgave();
        oppgave.setOppgaveId("1");
        when(oppgaveService.hentOppgaveMedFagSaksnummer(anyString())).thenReturn(oppgave);

        vedtakService.fattVedtak(behandlingID, BehandlingsresultatType.FASTSATT_LOVVALGSLAND);

        verify(behandlingRepository, times(1)).findOne(behandlingID);
        verify(prosessinstansRepo, times(1)).save(prosessinstansArgumentCaptor.capture());
        assertThat(prosessinstansArgumentCaptor.getValue().getType()).isEqualTo(ProsessType.IVERKSETT_VEDTAK);
        assertThat(prosessinstansArgumentCaptor.getValue().getSteg()).isEqualTo(ProsessSteg.IV_VALIDERING);
        verify(binge, times(1)).leggTil(any());
        verify(oppgaveService, times(1)).ferdigstillOppgave(oppgave.getOppgaveId());
    }

    @Test(expected = IkkeFunnetException.class)
    public void fattVedtak_behandlingIkkeFunnet() throws FunksjonellException, TekniskException {
        long behandlingID = 0L;
        vedtakService.fattVedtak(behandlingID, BehandlingsresultatType.FASTSATT_LOVVALGSLAND);
    }
}