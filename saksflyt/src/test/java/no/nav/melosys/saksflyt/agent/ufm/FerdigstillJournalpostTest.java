package no.nav.melosys.saksflyt.agent.ufm;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class FerdigstillJournalpostTest {

    @Mock
    private JoarkFasade joarkFasade;

    private FerdigstillJournalpost ferdigstillJournalpost;

    @Before
    public void setUp() {
        ferdigstillJournalpost = new FerdigstillJournalpost(joarkFasade);
    }

    @Test
    public void utfør_verifiserNesteSteg() throws Exception {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, "123");
        prosessinstans.setBehandling(new Behandling());
        ferdigstillJournalpost.utfør(prosessinstans);

        verify(joarkFasade).ferdigstillJournalføring(eq("123"));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.REG_UNNTAK_HENT_PERSON);
    }
}