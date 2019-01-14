package no.nav.melosys.saksflyt.agent.aou;

import no.nav.melosys.domain.*;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.ProsessSteg.AOU_OPPDATER_MEDL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterBehandlingsresultatTest {

    @Mock
    BehandlingsresultatRepository behandlingsresultatRepository;

    OppdaterBehandlingsresultat oppdaterBehandlingsresultat;

    @Before
    public void setUp() {
        oppdaterBehandlingsresultat = new OppdaterBehandlingsresultat(behandlingsresultatRepository);
    }

    @Test
    public void utfør() {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setType(Behandlingstype.SØKNAD);
        p.setType(ProsessType.ANMODNING_OM_UNNTAK);
        String testbruker = "Z097";
        p.setData(ProsessDataKey.SAKSBEHANDLER, testbruker);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatRepository.findOne(anyLong())).thenReturn(behandlingsresultat);

        oppdaterBehandlingsresultat.utfør(p);

        assertThat(behandlingsresultat.getType()).isEqualTo(BehandlingsresultatType.ANMODNING_OM_UNNTAK);
        assertThat(behandlingsresultat.getEndretAv()).isEqualTo(testbruker);
        assertThat(p.getSteg()).isEqualTo(AOU_OPPDATER_MEDL);
    }
}