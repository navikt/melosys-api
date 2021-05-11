package no.nav.melosys.service.vedtak;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.oppgave.OppgaveService;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import no.nav.melosys.service.vedtak.dto.FattFtrlVedtakRequest;
import no.nav.melosys.sikkerhet.context.SpringSubjectHandler;
import no.nav.melosys.sikkerhet.context.TestSubjectHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.Saksstatuser.MEDLEMSKAP_AVKLART;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper.MEDLEM_I_FOLKETRYGDEN;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.IVERKSETTER_VEDTAK;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FtrlVedtakServiceTest {

    public static final String SAKSNUMMER = "MEL-123";
    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    @Mock
    private BehandlingService behandlingService;

    @Mock
    private ProsessinstansService prosessinstansService;

    @Mock
    private OppgaveService oppgaveService;

    @Captor
    private ArgumentCaptor<Behandlingsresultat> behandlingsresultatCaptor;

    @Captor
    private ArgumentCaptor<Behandling> behandlingCaptor;

    private FtrlVedtakService ftrlVedtakService;

    @BeforeEach
    void setup() {
        ftrlVedtakService = new FtrlVedtakService(behandlingsresultatService, behandlingService, prosessinstansService, oppgaveService);

        SpringSubjectHandler.set(new TestSubjectHandler());
    }

    @Test
    void fattVedtak_Førstegangsvedtak_fatterVedtak() {
        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        FattFtrlVedtakRequest request = lagFattVedtakRequest();
        ftrlVedtakService.fattVedtak(lagBehandling(), request);

        verify(behandlingsresultatService).lagre(behandlingsresultatCaptor.capture());
        verify(behandlingService).lagre(behandlingCaptor.capture());
        verify(prosessinstansService).opprettProsessinstansIverksettVedtak(any(Behandling.class), eq(request));
        verify(oppgaveService).ferdigstillOppgaveMedSaksnummer(SAKSNUMMER);

        Behandlingsresultat lagretBehandlingsresultat = behandlingsresultatCaptor.getValue();
        assertThat(lagretBehandlingsresultat)
            .extracting("type", "begrunnelseFritekst", "fastsattAvLand")
            .containsExactly(MEDLEM_I_FOLKETRYGDEN, "Innvilget", Landkoder.NO);

        Behandling lagretBehandling = behandlingCaptor.getValue();
        assertThat(lagretBehandling.getStatus()).isEqualTo(IVERKSETTER_VEDTAK);
        assertThat(lagretBehandling.getFagsak().getStatus()).isEqualTo(MEDLEMSKAP_AVKLART);
    }

    private FattFtrlVedtakRequest lagFattVedtakRequest() {
        return new FattFtrlVedtakRequest.Builder()
            .medBehandlingsresultat(MEDLEM_I_FOLKETRYGDEN)
            .medVedtakstype(Vedtakstyper.FØRSTEGANGSVEDTAK)
            .medFritekstBegrunnelse("Innvilget")
            .build();
    }

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(123L);
        behandling.setFagsak(lagFagsak());
        return behandling;
    }

    private Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer(SAKSNUMMER);
        return fagsak;
    }
}
