package no.nav.melosys.saksflyt.agent.iv;

import java.util.Collections;
import java.util.Properties;

import no.nav.melosys.domain.*;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.FagsakRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;

import static no.nav.melosys.domain.ProsessSteg.STATUS_BEH_AVSL;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(MockitoJUnitRunner.class)
public class AvsluttFagsakOgBehandlingTest {

    private AvsluttFagsakOgBehandling agent;

    @Mock
    private BehandlingRepository behandlingRepository;

    @Mock
    private FagsakRepository fagsakRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;


    @Before
    public void setUp() {
        agent = new AvsluttFagsakOgBehandling(behandlingRepository, fagsakRepository, applicationEventPublisher);
    }

    @Test
    public void utfoerSteg() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setType(Behandlingstype.SØKNAD);
        p.setType(ProsessType.IVERKSETT_VEDTAK);
        Properties properties = new Properties();
        p.addData(properties);

        Behandling behandling = new Behandling();

        Fagsak fagsak = new Fagsak();
        fagsak.setBehandlinger( Collections.singletonList(behandling));

        behandling.setFagsak(fagsak);
        p.setBehandling(behandling);

        agent.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(STATUS_BEH_AVSL);
        assertThat(p.getBehandling().getStatus()).isEqualTo(Behandlingsstatus.AVSLUTTET);
        assertThat(p.getBehandling().getFagsak().getStatus()).isEqualTo(Fagsaksstatus.AVSLUTTET);

    }
} 