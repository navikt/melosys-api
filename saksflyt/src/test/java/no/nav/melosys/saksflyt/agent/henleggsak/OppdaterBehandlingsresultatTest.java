package no.nav.melosys.saksflyt.agent.henleggsak;

import java.util.Optional;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.ProsessSteg.HENLEGG_SAK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterBehandlingsresultatTest {
    private OppdaterBehandlingsresultat oppdaterBehandlingsresultat;

    @Captor
    private ArgumentCaptor<Behandlingsresultat> behandlingsresultatArgumentCaptor;

    @Mock
    BehandlingsresultatRepository behandlingsresultatRepository;

    @Before
    public void setUp() {
        oppdaterBehandlingsresultat = new OppdaterBehandlingsresultat(behandlingsresultatRepository);
    }

    @Rule
    public ExpectedException expectException = ExpectedException.none();

    @Test
    public void utfør() throws IkkeFunnetException {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = new Behandling();
        long behandlingId = 234234L;
        behandling.setId(behandlingId);
        prosessinstans.setBehandling(behandling);
        prosessinstans.setType(ProsessType.HENLEGG_SAK);
        String testbruker = "Z097";
        prosessinstans.setData(ProsessDataKey.SAKSBEHANDLER, testbruker);
        prosessinstans.setData(ProsessDataKey.BEHANDLINGSRESULTATTYPE, BehandlingsresultatType.HENLEGGELSE.getKode());

        Optional<Behandlingsresultat> behandlingsresultat = Optional.of(new Behandlingsresultat());
        doReturn(behandlingsresultat).when(behandlingsresultatRepository).findById(behandlingId);

        oppdaterBehandlingsresultat.utfør(prosessinstans);

        verify(behandlingsresultatRepository).save(behandlingsresultatArgumentCaptor.capture());
        Behandlingsresultat capture = behandlingsresultatArgumentCaptor.getValue();
        assertThat(capture.getType()).isEqualTo(BehandlingsresultatType.HENLEGGELSE);
        assertThat(capture.getEndretAv()).isEqualTo(testbruker);
        assertThat(prosessinstans.getSteg()).isEqualTo(HENLEGG_SAK);
    }

    @Test
    public void utførKasterExceptionNårBehandlingsresultatIkkeFunnet() throws IkkeFunnetException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(new Behandling());

        expectException.expect(IkkeFunnetException.class);
        oppdaterBehandlingsresultat.utfør(prosessinstans);
    }
}