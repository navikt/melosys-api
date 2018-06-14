package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Properties;

import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OpprettOppgaveTest {

    @Mock
    private GsakFasade gsakFasade;

    private OpprettOppgave agent;

    @Before
    public void setUp() {
        agent = new OpprettOppgave(gsakFasade);
    }

    @Test
    public void utfoerSteg() throws SikkerhetsbegrensningException {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_NY_SAK);
        Properties properties = new Properties();
        p.addData(properties);

        agent.utførSteg(p);

        verify(gsakFasade, times(1)).opprettOppgave(any());
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.FERDIG);
    }
}