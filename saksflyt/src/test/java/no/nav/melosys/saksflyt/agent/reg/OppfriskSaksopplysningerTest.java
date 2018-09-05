package no.nav.melosys.saksflyt.agent.reg;

import java.util.Properties;

import no.nav.melosys.domain.*;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OppfriskSaksopplysningerTest {

    @Mock
    private BehandlingRepository behandlingRepository;

    private OppfriskSaksopplysninger agent;

    @Before
    public void setUp() {
        agent = new OppfriskSaksopplysninger(behandlingRepository);
    }

    @Test
    public void utfoerSteg() throws SikkerhetsbegrensningException, FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        p.setBehandling(behandling);
        p.getBehandling().setType(BehandlingType.SØKNAD);

        agent.utførSteg(p);

        verify(behandlingRepository, times(1)).save(behandling);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.OPPRETT_OPPGAVE);
    }

    @Test
    public void oppfriskSaksopplysningSteg() throws SikkerhetsbegrensningException, FunksjonellException {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.OPPFRISKNING);
        Behandling behandling = new Behandling();
        p.setBehandling(behandling);
        p.addData(new Properties());
        p.setData(ProsessDataKey.OPPFRISK_SAKSOPPLYSNING, true);
        agent.utførSteg(p);

        assertThat(p.getSteg()).isNull();
    }
}