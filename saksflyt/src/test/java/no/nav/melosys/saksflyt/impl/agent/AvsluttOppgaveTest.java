package no.nav.melosys.saksflyt.impl.agent;

import java.util.Properties;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import no.nav.melosys.repository.ProsessinstansRepository;
import no.nav.melosys.saksflyt.api.Binge;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class AvsluttOppgaveTest {

    @Mock
    private Binge binge;

    @Mock
    private ProsessinstansRepository repo;

    @Mock
    private GsakFasade gsakFasade;

    private AvsluttOppgave agent;

    @Before
    public void setUp() {
        agent = new AvsluttOppgave(binge, repo, gsakFasade);
    }

    @Test
    public void utfoerSteg() throws SikkerhetsbegrensningException {
        Prosessinstans p = new Prosessinstans();
        Properties properties = new Properties();
        String oppgaveID = "123";
        properties.setProperty(ProsessDataKey.OPPGAVE_ID.getKode(), oppgaveID);
        p.addData(properties);

        agent.utførSteg(p);

        verify(gsakFasade, times(1)).ferdigstillOppgave(oppgaveID);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.VURDER_INNGANGSVILKÅR);
    }
}