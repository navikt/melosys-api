package no.nav.melosys.saksflyt.agent.jfr;

import java.util.Properties;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.SikkerhetsbegrensningException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.gsak.GsakFasade;
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
    private GsakFasade gsakFasade;

    private AvsluttOppgave agent;

    @Before
    public void setUp() {
        agent = new AvsluttOppgave(gsakFasade);
    }

    @Test
    public void utfoerSteg() throws MelosysException {
        Prosessinstans p = new Prosessinstans();
        p.setType(ProsessType.JFR_NY_SAK);
        Properties properties = new Properties();
        String oppgaveID = "123";
        properties.setProperty(ProsessDataKey.OPPGAVE_ID.getKode(), oppgaveID);
        p.addData(properties);

        agent.utførSteg(p);

        verify(gsakFasade, times(1)).ferdigstillOppgave(oppgaveID);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_AKTØR_ID);
    }
}