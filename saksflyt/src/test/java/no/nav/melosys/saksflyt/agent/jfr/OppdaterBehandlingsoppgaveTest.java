package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Collections;

import no.nav.melosys.domain.*;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
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
public class OppdaterBehandlingsoppgaveTest {

    private FagsakRepository fagsakRepository;

    private BehandlingRepository behandlingRepository;

    private GsakFasade gsakFasade;

    private OppdaterBehandlingsoppgave agent;

    private final static String AKTØR_ID = "Aktør-ID";
    private final static String SAKSNUMMER_FINNES_IKKE = "MELTEST-0";
    private final static String SAKSNUMMER_UTEN_BEHANDLING_OG_BRUKER = "MELTEST-1";
    private final static String SAKSNUMMER_MED_BRUKER = "MELTEST-2";
    private final static String SAKSNUMMER_MED_BEHANDLING_OG_BRUKER = "MELTEST-3";
    private final static String SAKSNUMMER_AVSLUTTET = "MELTEST-4";

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        fagsakRepository = mock(FagsakRepository.class);
        behandlingRepository = mock(BehandlingRepository.class);
        gsakFasade = mock(GsakFasade.class);
        agent = new OppdaterBehandlingsoppgave(fagsakRepository, behandlingRepository, gsakFasade);

        Fagsak fagsak = new Fagsak();

        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);

        Aktoer aktør = new Aktoer();
        aktør.setRolle(RolleType.BRUKER);
        aktør.setAktørId(AKTØR_ID);

        Fagsak fagsakMedBruker = new Fagsak();
        fagsakMedBruker.setAktører(Collections.singleton(aktør));

        Fagsak fagsakMedBehandlingOgBruker = new Fagsak();
        fagsakMedBehandlingOgBruker.setBehandlinger(Collections.singletonList(behandling));
        fagsakMedBehandlingOgBruker.setAktører(Collections.singleton(aktør));

        Fagsak fagsakAvsluttet = new Fagsak();
        fagsakAvsluttet.setStatus(Fagsaksstatus.AVSLUTTET);
        fagsakAvsluttet.setBehandlinger(Collections.singletonList(behandling));

        when(fagsakRepository.findBySaksnummer(SAKSNUMMER_UTEN_BEHANDLING_OG_BRUKER)).thenReturn(fagsak);
        when(fagsakRepository.findBySaksnummer(SAKSNUMMER_MED_BRUKER)).thenReturn(fagsakMedBruker);
        when(fagsakRepository.findBySaksnummer(SAKSNUMMER_MED_BEHANDLING_OG_BRUKER)).thenReturn(fagsakMedBehandlingOgBruker);
        when(fagsakRepository.findBySaksnummer(SAKSNUMMER_AVSLUTTET)).thenReturn(fagsakAvsluttet);
    }

    @Test
    public void manglerSaksnummer() {
        Prosessinstans p = new Prosessinstans();
        agent.utførSteg(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
        assertThat(p.getHendelser()).isNotEmpty();
        assertThat(p.getHendelser().get(0).getMelding()).isEqualTo("Prosessinstans 0 mangler saksnummer");
    }

    @Test
    public void fagsakFinnesIkke() {
        Prosessinstans p = new Prosessinstans();
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_FINNES_IKKE);
        agent.utførSteg(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
        assertThat(p.getHendelser()).isNotEmpty();
        assertThat(p.getHendelser().get(0).getMelding()).isEqualTo("Det finnes ingen fagsak med saksnummer " + SAKSNUMMER_FINNES_IKKE);
    }

    @Test
    public void fagsakUtenBehandlingUtenBruker() throws Exception {
        Prosessinstans p = new Prosessinstans();
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_UTEN_BEHANDLING_OG_BRUKER);
        agent.utførSteg(p);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
        assertThat(p.getHendelser()).isNotEmpty();
        assertThat(p.getHendelser().get(0).getMelding()).isEqualTo("Det finnes ingen bruker på fagsak med saksnummer " + SAKSNUMMER_UTEN_BEHANDLING_OG_BRUKER);
    }

    @Test
    public void fagsakUtenBehandlingMedBruker() throws Exception {
        Prosessinstans p = new Prosessinstans();
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_MED_BRUKER);
        agent.utførSteg(p);
        verify(gsakFasade, times(1)).opprettSak(SAKSNUMMER_MED_BRUKER, Behandlingstype.SØKNAD, AKTØR_ID);
        verify(fagsakRepository, times(1)).save(any(Fagsak.class));
        verify(behandlingRepository, times(1)).save(any(Behandling.class));
    }

    @Test
    public void fagsakMedBehandlingOgBruker() throws Exception {
        Prosessinstans p = new Prosessinstans();
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_MED_BEHANDLING_OG_BRUKER);
        agent.utførSteg(p);
        verify(gsakFasade, times(0)).opprettSak(SAKSNUMMER_MED_BEHANDLING_OG_BRUKER, Behandlingstype.SØKNAD, AKTØR_ID);
        verify(fagsakRepository, times(1)).save(any(Fagsak.class));
        verify(behandlingRepository, times(1)).save(any(Behandling.class));
    }

    @Test
    public void fagsakAvsluttet() {
        doAnswer(a -> {
            Fagsak fagsak = a.getArgument(0);
            assertThat(fagsak.getStatus()).isEqualTo(Fagsaksstatus.OPPRETTET);
            return fagsak;
        }).when(fagsakRepository).save(any(Fagsak.class));

        Prosessinstans p = new Prosessinstans();
        p.setData(ProsessDataKey.SAKSNUMMER, SAKSNUMMER_AVSLUTTET);
        agent.utførSteg(p);
        verify(fagsakRepository, times(1)).save(any(Fagsak.class));
        verify(behandlingRepository, times(1)).save(any(Behandling.class));
    }
}
