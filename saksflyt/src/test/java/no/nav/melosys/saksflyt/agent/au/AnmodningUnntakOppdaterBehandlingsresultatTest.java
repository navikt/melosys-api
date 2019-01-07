package no.nav.melosys.saksflyt.agent.au;

import no.nav.melosys.domain.*;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.ProsessSteg.AU_OPPDATER_MEDL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AnmodningUnntakOppdaterBehandlingsresultatTest {

    @Mock
    BehandlingsresultatRepository behandlingsresultatRepository;

    AnmodningUnntakOppdaterBehandlingsresultat oppdaterBehandlingsresultat;

    @Captor
    private ArgumentCaptor<Behandlingsresultat> behandlingsresultatArgumentCaptor;

    @Before
    public void setUp() {
        oppdaterBehandlingsresultat = new AnmodningUnntakOppdaterBehandlingsresultat(behandlingsresultatRepository);
    }

    @Test
    public void utfør() {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setType(Behandlingstype.SØKNAD);
        p.setType(ProsessType.ANMODNING_UNNTAK);
        String testbruker = "Z097";
        p.setData(ProsessDataKey.SAKSBEHANDLER, testbruker);
        p.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, BehandlingsresultatType.ANMODNING_OM_UNNTAK.getKode());

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatRepository.findOne(anyLong())).thenReturn(behandlingsresultat);

        oppdaterBehandlingsresultat.utfør(p);

        verify(behandlingsresultatRepository).save(behandlingsresultatArgumentCaptor.capture());
        assertThat(behandlingsresultat.getType()).isEqualTo(BehandlingsresultatType.ANMODNING_OM_UNNTAK);
        assertThat(behandlingsresultat.getEndretAv()).isEqualTo(testbruker);
        assertThat(p.getSteg()).isEqualTo(AU_OPPDATER_MEDL);
    }
}