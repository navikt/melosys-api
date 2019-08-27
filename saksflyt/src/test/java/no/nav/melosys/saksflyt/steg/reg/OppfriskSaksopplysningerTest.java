package no.nav.melosys.saksflyt.steg.reg;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.repository.BehandlingRepository;
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
    public void utfoerSteg() throws FunksjonellException, TekniskException {
        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        p.setBehandling(behandling);
        p.getBehandling().setType(Behandlingstyper.SOEKNAD);

        agent.utførSteg(p);

        verify(behandlingRepository, times(1)).save(behandling);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.GSAK_OPPRETT_OPPGAVE);
    }

    @Test
    public void oppfriskSaksopplysningSteg() throws FunksjonellException {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.OPPFRISKNING);
        Behandling behandling = new Behandling();
        p.setBehandling(behandling);
        agent.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(ProsessSteg.FERDIG);
    }
}