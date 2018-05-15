package no.nav.melosys.saksflyt.impl.agent;

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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class OppdaterJournalpostTest {

    @Mock
    private Binge binge;

    @Mock
    private ProsessinstansRepository repo;

    @Mock
    private JoarkFasade joarkFasade;

    private OppdaterJournalpost agent;

    @Before
    public void setUp() throws Exception {
        agent = new OppdaterJournalpost(binge, repo, joarkFasade);
    }

    @Test
    public void utfoerSteg() throws SikkerhetsbegrensningException {
        Prosessinstans p = new Prosessinstans();

        agent.utfoerSteg(p);

        verify(joarkFasade, times(1)).oppdaterJounalpost(any(), any(), any(), any(), any(), any());
        assertThat(p.getSteg()).isEqualTo(ProsessSteg.JFR_FERDIGSTILL_JOURNALPOST);
    }
}