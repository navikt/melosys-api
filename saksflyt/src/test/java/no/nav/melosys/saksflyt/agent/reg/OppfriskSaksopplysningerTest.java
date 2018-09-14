package no.nav.melosys.saksflyt.agent.reg;

import java.util.Properties;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.datavarehus.BehandlingLagretEvent;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.saksflyt.agent.OppfriskSaksopplysninger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OppfriskSaksopplysningerTest {

    @Mock
    private BehandlingRepository behandlingRepository;

    private OppfriskSaksopplysninger agent;

    private ApplicationEventPublisher applicationEventPublisher;

    @Before
    public void setUp() {
        applicationEventPublisher = mock(ApplicationEventPublisher.class);
        agent = new OppfriskSaksopplysninger(behandlingRepository, applicationEventPublisher);
    }

    @Test
    public void utfoerSteg() throws SikkerhetsbegrensningException, FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        p.setBehandling(behandling);
        p.getBehandling().setType(Behandlingstype.SØKNAD);

        agent.utførSteg(p);

        verify(behandlingRepository, times(1)).save(behandling);
        verify(applicationEventPublisher).publishEvent(any(BehandlingLagretEvent.class));
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.OPPRETT_OPPGAVE);
    }

    @Test
    public void oppfriskSaksopplysningSteg() throws SikkerhetsbegrensningException, FunksjonellException {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.OPPFRISKNING);
        Behandling behandling = new Behandling();
        p.setBehandling(behandling);
        p.addData(new Properties());
        agent.utførSteg(p);

        assertThat(p.getSteg()).isNull();
    }
}