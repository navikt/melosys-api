package no.nav.melosys.saksflyt.agent.iv;

import java.time.LocalDate;

import no.nav.melosys.domain.*;
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

    OppdaterBehandlingsresultat oppdaterBehandlingsresultat;

    @Captor
    private ArgumentCaptor<Behandlingsresultat> behandlingsresultatArgumentCaptor;

    @Before
    public void setUp() {
        oppdaterBehandlingsresultat = new OppdaterBehandlingsresultat(behandlingsresultatRepository);
    }

    @Test
    public void utfør() {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setType(Behandlingstype.SØKNAD);
        p.setType(ProsessType.IVERKSETT_VEDTAK);
        String testbruker = "Z097";
        p.setData(ProsessDataKey.SAKSBEHANDLER, testbruker);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatRepository.findOne(anyLong())).thenReturn(behandlingsresultat);

        oppdaterBehandlingsresultat.utfør(p);

        verify(behandlingsresultatRepository).save(behandlingsresultatArgumentCaptor.capture());
        assertThat(behandlingsresultat.getType()).isEqualTo(BehandlingsresultatType.FASTSATT_LOVVALGSLAND);
        assertThat(behandlingsresultat.getEndretAv()).isEqualTo(testbruker);
        assertThat(behandlingsresultat.getVedtaksdato()).isNotNull();
        assertThat(behandlingsresultat.getVedtakKlagefrist()).isEqualTo(LocalDate.now().plusWeeks(OppdaterBehandlingsresultat.FRIST_KLAGE_UKER));
        assertThat(p.getSteg()).isEqualTo(IV_OPPDATER_MEDL);
    }
}