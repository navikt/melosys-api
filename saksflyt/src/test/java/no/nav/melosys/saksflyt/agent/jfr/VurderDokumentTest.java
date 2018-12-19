package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Collections;

import no.nav.melosys.domain.*;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class VurderDokumentTest {

    private FagsakRepository fagsakRepository;

    private BehandlingRepository behandlingRepository;

    private VurderDokument agent;

    private final static String SAKSNUMMER_FINNES_IKKE = "MELTEST-0";
    private final static String SAKSNUMMER_UTEN_BEHANDLING = "MELTEST-1";
    private final static String SAKSNUMMER_MED_BEHANDLING = "MELTEST-2";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        fagsakRepository = mock(FagsakRepository.class);
        behandlingRepository = mock(BehandlingRepository.class);
        agent = new VurderDokument(fagsakRepository, behandlingRepository);

        Fagsak fagsak = new Fagsak();

        Fagsak fagsakMedBehandling = new Fagsak();
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        fagsakMedBehandling.setBehandlinger(Collections.singletonList(behandling));

        when(fagsakRepository.findBySaksnummer(SAKSNUMMER_UTEN_BEHANDLING)).thenReturn(fagsak);
        when(fagsakRepository.findBySaksnummer(SAKSNUMMER_MED_BEHANDLING)).thenReturn(fagsakMedBehandling);
    }

    @Test
    public void fagsakFinnesIkke() {
        Prosessinstans p = new Prosessinstans();
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_FINNES_IKKE);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstype.SØKNAD);
        agent.utførSteg(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
        assertThat(p.getHendelser()).isNotEmpty();
        assertThat(p.getHendelser().get(0).getMelding()).isEqualTo("Det finnes ingen fagsak med saksnummer " + SAKSNUMMER_FINNES_IKKE);
    }

    @Test
    public void fagsakMedBehandling() {
        Prosessinstans p = new Prosessinstans();
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_MED_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstype.SØKNAD);
        agent.utførSteg(p);
        verify(behandlingRepository, times(1)).save(any(Behandling.class));
        assertThat(p.getSteg()).isNull();
    }

    @Test
    public void fagsakUtenBehandling() {
        Prosessinstans p = new Prosessinstans();
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_UTEN_BEHANDLING);
        p.setData(ProsessDataKey.BEHANDLINGSTYPE, Behandlingstype.SØKNAD);
        agent.utførSteg(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_AKTØR_ID);
    }
}
