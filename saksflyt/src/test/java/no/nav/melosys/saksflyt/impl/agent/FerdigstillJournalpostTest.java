package no.nav.melosys.saksflyt.impl.agent;

import java.util.Properties;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.integrasjon.felles.exception.SikkerhetsbegrensningException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
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
public class FerdigstillJournalpostTest {

    @Mock
    private Binge binge;

    @Mock
    private ProsessinstansRepository repo;

    @Mock
    private JoarkFasade joarkFasade;

    private FerdigstillJournalpost agent;

    @Before
    public void setUp() {
        agent = new FerdigstillJournalpost(binge, repo, joarkFasade);
    }

    @Test
    public void utfoerSteg() throws SikkerhetsbegrensningException {
        Prosessinstans p = new Prosessinstans();
        Properties properties = new Properties();
        String journalpostID = "Journal_ID";
        properties.setProperty(ProsessDataKey.JOURNALPOST_ID, journalpostID);
        p.addData(properties);

        agent.utfoerSteg(p);

        verify(joarkFasade, times(1)).ferdigstillJournalføring(journalpostID);
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_AVSLUTT_OPPGAVE);
    }
}