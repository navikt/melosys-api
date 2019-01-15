package no.nav.melosys.saksflyt.agent.iv;

import java.time.LocalDate;
import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.Behandlingstyper;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.ProsessSteg.IV_OPPDATER_MEDL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterBehandlingsresultatTest {

    @Mock
    BehandlingsresultatRepository behandlingsresultatRepository;

    private OppdaterBehandlingsresultat oppdaterBehandlingsresultat;

    @Captor
    private ArgumentCaptor<Behandlingsresultat> behandlingsresultatArgumentCaptor;

    @Before
    public void setUp() {
        oppdaterBehandlingsresultat = new OppdaterBehandlingsresultat(behandlingsresultatRepository);
    }

    @Test
    public void utfør() {
        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        p.setBehandling(behandling);
        p.setBehandling(new Behandling());
        p.getBehandling().setType(Behandlingstyper.SOEKNAD);
        p.setType(ProsessType.IVERKSETT_VEDTAK);
        String testbruker = "Z097";
        p.setData(ProsessDataKey.SAKSBEHANDLER, testbruker);
        p.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND.getKode());

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatRepository.findById(anyLong())).thenReturn(Optional.of(behandlingsresultat));

        oppdaterBehandlingsresultat.utfør(p);

        verify(behandlingsresultatRepository).save(behandlingsresultatArgumentCaptor.capture());
        Behandlingsresultat capture = behandlingsresultatArgumentCaptor.getValue();
        assertThat(capture.getType()).isEqualTo(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        assertThat(capture.getEndretAv()).isEqualTo(testbruker);
        assertThat(capture.getVedtaksdato()).isNotNull();
        assertThat(capture.getVedtakKlagefrist()).isEqualTo(LocalDate.now().plusWeeks(OppdaterBehandlingsresultat.FRIST_KLAGE_UKER));
        assertThat(p.getSteg()).isEqualTo(IV_OPPDATER_MEDL);
    }
}