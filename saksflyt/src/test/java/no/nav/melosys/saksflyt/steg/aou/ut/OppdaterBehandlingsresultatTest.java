package no.nav.melosys.saksflyt.steg.aou.ut;

import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.ProsessSteg.AOU_AVKLAR_MYNDIGHET;
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
    public void utfør() throws IkkeFunnetException {
        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        p.setBehandling(behandling);
        p.getBehandling().setType(Behandlingstyper.SOEKNAD);
        p.setType(ProsessType.ANMODNING_OM_UNNTAK);
        String testbruker = "Z097";
        p.setData(ProsessDataKey.SAKSBEHANDLER, testbruker);

        Behandlingsresultat behandlingsresultat = new Behandlingsresultat();
        when(behandlingsresultatRepository.findById(anyLong())).thenReturn(Optional.of(behandlingsresultat));

        oppdaterBehandlingsresultat.utfør(p);

        assertThat(behandlingsresultat.getType()).isEqualTo(Behandlingsresultattyper.ANMODNING_OM_UNNTAK);
        assertThat(behandlingsresultat.getEndretAv()).isEqualTo(testbruker);
        assertThat(p.getSteg()).isEqualTo(AOU_AVKLAR_MYNDIGHET);
    }
}