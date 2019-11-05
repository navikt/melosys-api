package no.nav.melosys.saksflyt.steg.jfr.sed.brev;

import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.kodeverk.Avsendertyper;
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
    private static final Long GSAK_SAKSNUMMER = 111L;
    private static final String TITTEL = "tittel";
    private static final String AVSENDER_ID = "avsenderID";
    private static final String AVSENDER_NAVN = "avsenderNavn";

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
        prosessinstans.setData(ProsessDataKey.AVSENDER_TYPE, Avsendertyper.UTENLANDSK_TRYGDEMYNDIGHET);
        prosessinstans.setData(ProsessDataKey.AVSENDER_ID, AVSENDER_ID);
        prosessinstans.setData(ProsessDataKey.AVSENDER_NAVN, AVSENDER_NAVN);

        ferdigstillJournalpost.utfør(prosessinstans);

        JournalpostOppdatering forventetOppdatering = new JournalpostOppdatering.Builder()
            .medBrukerID(BRUKER_ID).medArkivSakID(GSAK_SAKSNUMMER).medTittel(TITTEL)
            .medAvsenderType(Avsendertyper.UTENLANDSK_TRYGDEMYNDIGHET).medAvsenderID(AVSENDER_ID)
            .medAvsenderNavn(AVSENDER_NAVN).build();
        verify(joarkFasade).oppdaterJournalpost(eq(JOURNALPOST_ID), eq(forventetOppdatering), eq(true));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.JFR_AOU_BREV_OPPRETT_SEDDOKUMENT);
    }
}