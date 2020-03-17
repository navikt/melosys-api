package no.nav.melosys.saksflyt.steg.jfr;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.ArrayList;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Saksstatuser;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.service.behandling.BehandlingService;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static no.nav.melosys.domain.kodeverk.Saksstatuser.OPPRETTET;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.FEILET_MASKINELT;
import static no.nav.melosys.domain.saksflyt.ProsessSteg.GSAK_OPPRETT_OPPGAVE;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.*;

public class ReplikerBehandlingTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    private ReplikerBehandling agent;
    private Prosessinstans p;
    private Fagsak fagsak;
    private FagsakRepository fagsakRepository;
    private BehandlingService behandlingService;

    @Before
    public void setUp() {
        behandlingService = mock(BehandlingService.class);
        fagsakRepository = mock(FagsakRepository.class);
        agent = new ReplikerBehandling(fagsakRepository, behandlingService);
        fagsak = new Fagsak();
        fagsak.setStatus(Saksstatuser.LOVVALG_AVKLART);
        fagsak.setBehandlinger(new ArrayList<>());
        p = new Prosessinstans();
        p.setData(ProsessDataKey.SAKSNUMMER, "MelTest-1");
        doReturn(fagsak).when(fagsakRepository).findBySaksnummer("MelTest-1");

    }

    @Test
    public void utfør_manglendeInaktivBehandling_feiler() throws FunksjonellException, TekniskException {
        expectedException.expect(FunksjonellException.class);
        agent.utfør(p);
        assertThat(p.getSteg()).isEqualTo(FEILET_MASKINELT);
    }

    @Test
    public void utfør_eksisterendeInaktivBehandling_settStegOpprettOppgave() throws FunksjonellException, TekniskException, NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        behandling.setRegistrertDato(Instant.now());
        fagsak.getBehandlinger().add(behandling);
        Behandling replikertBehandling = new Behandling();
        replikertBehandling.setId(11L);
        doReturn(replikertBehandling).when(behandlingService).replikerBehandlingOgBehandlingsresultat(behandling,  Behandlingsstatus.OPPRETTET, Behandlingstyper.ENDRET_PERIODE);

        agent.utfør(p);

        verify(fagsakRepository).save(fagsak);
        assertThat(fagsak.getStatus()).isEqualTo(OPPRETTET);
        assertThat(p.getSteg()).isEqualTo(GSAK_OPPRETT_OPPGAVE);
        assertThat(p.getBehandling()).isEqualTo(replikertBehandling);
    }
}