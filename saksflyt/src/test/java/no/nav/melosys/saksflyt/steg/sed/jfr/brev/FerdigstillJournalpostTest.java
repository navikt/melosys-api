package no.nav.melosys.saksflyt.steg.sed.jfr.brev;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.ProsessType;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
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
    public void setup() {
        ferdigstillJournalpost = new FerdigstillJournalpost(joarkFasade);
    }

    @Test
    public void utfør() throws MelosysException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, "123");
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, "1234");
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, "111");

        ferdigstillJournalpost.utfør(prosessinstans);

        verify(joarkFasade).oppdaterJournalpostMedSaksnummerOgBruker(eq("123"), eq("1234"), eq(111L), eq(true));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.AOU_MOTTAK_OPPRETT_ANMODNINGSPERIODE);
        assertThat(prosessinstans.getType()).isEqualTo(ProsessType.ANMODNING_OM_UNNTAK_MOTTAK);
    }
}