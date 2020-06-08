package no.nav.melosys.saksflyt.steg.iv;

import java.time.LocalDate;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.Vedtakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.saksflyt.ProsessSteg.IV_OPPRETT_AVGIFTSOPPGAVE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterBehandlingsresultatTest {
    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    private OppdaterBehandlingsresultat oppdaterBehandlingsresultat;

    @Captor
    private ArgumentCaptor<Behandlingsresultat> behandlingsresultatArgumentCaptor;

    @Before
    public void setUp() {
        oppdaterBehandlingsresultat = new OppdaterBehandlingsresultat(behandlingsresultatService);
    }

    @Test
    public void utfør() throws FunksjonellException {
        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        p.setBehandling(behandling);
        p.getBehandling().setType(Behandlingstyper.SOEKNAD);
        p.setType(ProsessType.IVERKSETT_VEDTAK);
        String testbruker = "Z097";
        p.setData(ProsessDataKey.SAKSBEHANDLER, testbruker);
        p.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND.getKode());
        p.setData(ProsessDataKey.VEDTAKSTYPE, Vedtakstyper.KORRIGERT_VEDTAK.getKode());
        p.setData(ProsessDataKey.REVURDER_BEGRUNNELSE, "BEGRUNNELSE");

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        oppdaterBehandlingsresultat.utfør(p);

        verify(behandlingsresultatService).lagre(behandlingsresultatArgumentCaptor.capture());
        Behandlingsresultat capture = behandlingsresultatArgumentCaptor.getValue();
        assertThat(capture.getType()).isEqualTo(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        assertThat(capture.getEndretAv()).isEqualTo(testbruker);
        assertThat(capture.getVedtakMetadata().getVedtaksdato()).isNotNull();
        assertThat(capture.getVedtakMetadata().getVedtakKlagefrist()).isEqualTo(LocalDate.now().plusWeeks(OppdaterBehandlingsresultat.FRIST_KLAGE_UKER));
        assertThat(capture.getVedtakMetadata().getVedtakstype()).isEqualTo(Vedtakstyper.KORRIGERT_VEDTAK);
        assertThat(capture.getVedtakMetadata().getRevurderBegrunnelse()).isEqualTo("BEGRUNNELSE");
        assertThat(p.getSteg()).isEqualTo(IV_OPPRETT_AVGIFTSOPPGAVE);
    }

    @Test
    public void utfør_annenProsesstypeEnnIverksettVedtak_setterIkkeResultattypeOgLand() throws FunksjonellException {
        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        p.setBehandling(behandling);
        p.getBehandling().setType(Behandlingstyper.SOEKNAD);
        p.setType(ProsessType.IVERKSETT_VEDTAK_FORKORT_PERIODE);
        String testbruker = "Z097";
        p.setData(ProsessDataKey.SAKSBEHANDLER, testbruker);
        p.setData(ProsessDataKey.VEDTAKSTYPE, Vedtakstyper.KORRIGERT_VEDTAK.getKode());
        p.setData(ProsessDataKey.REVURDER_BEGRUNNELSE, "BEGRUNNELSE");

        Behandlingsresultat behandlingsresultat = spy(new Behandlingsresultat());
        when(behandlingsresultatService.hentBehandlingsresultat(anyLong())).thenReturn(behandlingsresultat);

        oppdaterBehandlingsresultat.utfør(p);

        verify(behandlingsresultat, never()).setType(any());
        verify(behandlingsresultat, never()).setFastsattAvLand(any());

        verify(behandlingsresultatService).lagre(behandlingsresultatArgumentCaptor.capture());
        Behandlingsresultat capture = behandlingsresultatArgumentCaptor.getValue();
        assertThat(capture.getEndretAv()).isEqualTo(testbruker);
        assertThat(capture.getVedtakMetadata().getVedtaksdato()).isNotNull();
        assertThat(capture.getVedtakMetadata().getVedtakKlagefrist()).isEqualTo(LocalDate.now().plusWeeks(OppdaterBehandlingsresultat.FRIST_KLAGE_UKER));
        assertThat(capture.getVedtakMetadata().getVedtakstype()).isEqualTo(Vedtakstyper.KORRIGERT_VEDTAK);
        assertThat(capture.getVedtakMetadata().getRevurderBegrunnelse()).isEqualTo("BEGRUNNELSE");
        assertThat(p.getSteg()).isEqualTo(IV_OPPRETT_AVGIFTSOPPGAVE);
    }
}