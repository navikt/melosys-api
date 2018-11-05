package no.nav.melosys.service.vedtak;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
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

    private VedtakService vedtakService;

    @Captor
    private ArgumentCaptor<Prosessinstans> prosessinstansArgumentCaptor;

    @Before
    public void setUp() {
        vedtakService = new VedtakService(behandlingRepository, binge, prosessinstansRepo);
        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    public void fattVedtak() throws IkkeFunnetException {
        Long behandlingID = 1L;
        Behandling behandling = new Behandling();
        when(behandlingRepository.findOne(behandlingID)).thenReturn(behandling);

        vedtakService.fattVedtak(behandlingID);

        verify(behandlingRepository, times(1)).findOne(behandlingID);
        verify(prosessinstansRepo, times(1)).save(prosessinstansArgumentCaptor.capture());
        assertThat(prosessinstansArgumentCaptor.getValue().getType()).isEqualTo(ProsessType.IVERKSETT_VEDTAK);
        assertThat(prosessinstansArgumentCaptor.getValue().getSteg()).isEqualTo(ProsessSteg.IV_VALIDERING);
        verify(binge, times(1)).leggTil(any());
    }

    @Test(expected = IkkeFunnetException.class)
    public void fattVedtak_behandlingIkkeFunnet() throws IkkeFunnetException {
        long behandlingID = 0L;
        vedtakService.fattVedtak(behandlingID);
    }
}