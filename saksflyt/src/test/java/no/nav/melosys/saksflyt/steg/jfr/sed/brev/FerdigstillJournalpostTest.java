package no.nav.melosys.saksflyt.steg.jfr.sed.brev;

import no.nav.melosys.domain.ProsessDataKey;
import no.nav.melosys.domain.ProsessSteg;
import no.nav.melosys.domain.Prosessinstans;
import no.nav.melosys.exception.MelosysException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.joark.JournalpostOppdatering;
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

    private static final String JOURNALPOST_ID = "jp123";
    private static final String BRUKER_ID = "bruker1234";
    private static final String AKTØR_ID = "aktør123";
    private static final Long GSAK_SAKSNUMMER = 111L;
    private static final String TITTEL = "tittel";

    @Before
    public void setup() {
        ferdigstillJournalpost = new FerdigstillJournalpost(joarkFasade);
    }

    @Test
    public void utfør() throws MelosysException {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, JOURNALPOST_ID);
        prosessinstans.setData(ProsessDataKey.BRUKER_ID, BRUKER_ID);
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, GSAK_SAKSNUMMER.toString());
        prosessinstans.setData(ProsessDataKey.HOVEDDOKUMENT_TITTEL, TITTEL);

        ferdigstillJournalpost.utfør(prosessinstans);

        JournalpostOppdatering forventetOppdatering = new JournalpostOppdatering.Builder()
            .medBrukerID(BRUKER_ID).medArkivSakID(GSAK_SAKSNUMMER).medTittel(TITTEL).build();
        verify(joarkFasade).oppdaterJournalpost(eq(JOURNALPOST_ID), eq(forventetOppdatering), eq(true));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.JFR_AOU_BREV_OPPRETT_SEDDOKUMENT);
    }
}