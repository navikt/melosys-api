package no.nav.melosys.saksflyt.steg.reg;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.repository.BehandlingRepository;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class VurderGjenbrukOppgaveTest {

    @Mock
    private BehandlingRepository behandlingRepository;

    private VurderGjenbrukOppgave agent;

    @Before
    public void setUp() {
        agent = new VurderGjenbrukOppgave(behandlingRepository);
    }

    @Test
    public void utfør_opprettOppgave() {
        Prosessinstans p = new Prosessinstans();
        Behandling behandling = new Behandling();
        p.setBehandling(behandling);
        p.getBehandling().setType(Behandlingstyper.SOEKNAD);

        agent.utfør(p);

        verify(behandlingRepository).save(behandling);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.GSAK_OPPRETT_OPPGAVE);
    }

    @Test
    public void utfør_gjenbrukOppgave() {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.OPPRETT_NY_SAK);
        Behandling behandling = new Behandling();
        p.setBehandling(behandling);

        agent.utfør(p);

        verify(behandlingRepository).save(behandling);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.GJENBRUK_OPPGAVE);
    }
}