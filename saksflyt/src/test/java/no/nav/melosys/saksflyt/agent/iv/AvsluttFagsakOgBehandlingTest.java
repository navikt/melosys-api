package no.nav.melosys.saksflyt.agent.iv;

import java.util.Collections;

import no.nav.melosys.domain.*;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static no.nav.melosys.domain.ProsessSteg.IV_STATUS_BEH_AVSL;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class AvsluttFagsakOgBehandlingTest {

    private AvsluttFagsakOgBehandling agent;

    @Mock
    private BehandlingRepository behandlingRepository;

    @Mock
    private FagsakRepository fagsakRepository;


    @Before
    public void setUp() {
        agent = new AvsluttFagsakOgBehandling(behandlingRepository, fagsakRepository);
    }

    @Test
    public void utfoerSteg() {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setType(Behandlingstype.SØKNAD);
        p.setType(ProsessType.IVERKSETT_VEDTAK);

        Behandling behandling = new Behandling();

        Fagsak fagsak = new Fagsak();
        fagsak.setSaksnummer("MEL-112");
        fagsak.setBehandlinger(Collections.singletonList(behandling));

        behandling.setFagsak(fagsak);
        p.setBehandling(behandling);

        agent.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(IV_STATUS_BEH_AVSL);
        assertThat(p.getBehandling().getStatus()).isEqualTo(Behandlingsstatus.AVSLUTTET);
        assertThat(p.getBehandling().getFagsak().getStatus()).isEqualTo(Fagsaksstatus.AVSLUTTET);

    }
} 