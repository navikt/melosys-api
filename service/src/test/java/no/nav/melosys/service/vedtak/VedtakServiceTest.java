package no.nav.melosys.service.vedtak;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.Endretperioder;
import no.nav.melosys.domain.oppgave.Oppgave;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class VedtakServiceTest {
    @Mock
    private BehandlingRepository behandlingRepository;
    @Mock
    private OppgaveService oppgaveService;
    @Mock
    private ProsessinstansService prosessinstansService;

    private VedtakService vedtakService;

    private long behandlingID;

    @Before
    public void setUp() {
        vedtakService = new VedtakService(behandlingRepository, oppgaveService, prosessinstansService);
        SpringSubjectHandler.set(new TestSubjectHandler());

        behandlingID = 1L;
        Behandling behandling = new Behandling();

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-111");
        behandling.setFagsak(fagsak);

        when(behandlingRepository.findById(behandlingID)).thenReturn(Optional.of(behandling));
    }

    @Test
    public void fattVedtak_fungerer() throws FunksjonellException, TekniskException {
        Oppgave oppgave = new Oppgave();
        oppgave.setOppgaveId("1");
        Behandlingsresultattyper resultatType = Behandlingsresultattyper.FASTSATT_LOVVALGSLAND;

        vedtakService.fattVedtak(behandlingID, resultatType);

        verify(behandlingRepository).findById(behandlingID);
        verify(prosessinstansService).opprettProsessinstansIverksettVedtak(any(Behandling.class), eq(resultatType));

        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(any());

        Optional<Behandling> behandling = behandlingRepository.findById(behandlingID);
        assertThat(behandling).isPresent();
        assertThat(behandling.get().getStatus()).isEqualTo(Behandlingsstatus.IVERKSETTER_VEDTAK);
    }

    @Test(expected = IkkeFunnetException.class)
    public void fattVedtak_behandlingIkkeFunnet() throws FunksjonellException, TekniskException {
        long behandlingID = 0L;
        vedtakService.fattVedtak(behandlingID, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
    }

    @Test
    public void endreVedtak_fungerer() throws FunksjonellException, TekniskException {
        vedtakService.endreVedtak(behandlingID, Endretperioder.ENDRINGER_ARBEIDSSITUASJON);

        verify(behandlingRepository).findById(behandlingID);
        verify(prosessinstansService).opprettProsessinstansForkortPeriode(any(Behandling.class), eq(Endretperioder.ENDRINGER_ARBEIDSSITUASJON));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(any());
    }
}