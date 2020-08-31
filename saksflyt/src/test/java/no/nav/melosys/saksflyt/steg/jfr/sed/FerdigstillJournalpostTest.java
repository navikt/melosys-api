package no.nav.melosys.saksflyt.steg.jfr.sed;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessSteg;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.joark.JournalpostOppdatering;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FerdigstillJournalpostTest {
    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private TpsFasade tpsFasade;

    private FerdigstillJournalpost ferdigstillJournalpost;

    private static final String JOURNALPOST_ID = "jp123";
    private static final String BRUKER_ID = "bruker123";
    private static final String AKTØR_ID = "aktør123";
    private static final Long GSAK_SAKSNUMMER = 123L;
    private static final String TITTEL = "tittel123";
    private static final String DOKUMENT_ID = "dokID123";

    @Before
    public void setUp() {
        ferdigstillJournalpost = new FerdigstillJournalpost(joarkFasade, tpsFasade);
    }

    @Test
    public void utfør_ikkeProsesstypeJournalfør_skalBehandlesVidere() throws Exception {

        Prosessinstans prosessinstans = hentProsessinstans();
        prosessinstans.setType(ProsessType.REGISTRERING_UNNTAK);
        when(tpsFasade.hentIdentForAktørId(eq(AKTØR_ID))).thenReturn(BRUKER_ID);
        ferdigstillJournalpost.utfør(prosessinstans);

        JournalpostOppdatering forventetOppdatering = new JournalpostOppdatering.Builder()
            .medBrukerID(BRUKER_ID).medArkivSakID(GSAK_SAKSNUMMER).medHovedDokumentID(DOKUMENT_ID).medTittel(TITTEL).build();
        verify(joarkFasade).oppdaterJournalpost(eq(JOURNALPOST_ID), eq(forventetOppdatering), eq(true));
        verify(tpsFasade).hentIdentForAktørId(eq(AKTØR_ID));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.hentFørsteProsessStegForType(prosessinstans.getType()));
    }

    @Test
    public void utfør_ikkeProsesstypeJournalfør_skalIkkeBehandlesVidere() throws Exception {

        Prosessinstans prosessinstans = hentProsessinstans();
        prosessinstans.setType(ProsessType.MOTTAK_SED_JOURNALFØRING);

        when(tpsFasade.hentIdentForAktørId(eq(AKTØR_ID))).thenReturn(BRUKER_ID);
        ferdigstillJournalpost.utfør(prosessinstans);

        JournalpostOppdatering forventetOppdatering = new JournalpostOppdatering.Builder()
            .medBrukerID(BRUKER_ID).medArkivSakID(GSAK_SAKSNUMMER).medHovedDokumentID(DOKUMENT_ID).medTittel(TITTEL).build();
        verify(joarkFasade).oppdaterJournalpost(eq(JOURNALPOST_ID), eq(forventetOppdatering), eq(true));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);
    }

    @Test
    public void utfør_prosesstypeOpprettNySakSedForespørsel_skalIkkeFerdigstilleJournalpost() throws FunksjonellException, TekniskException {
        Prosessinstans prosessinstans = hentProsessinstans();
        prosessinstans.setType(ProsessType.OPPRETT_NY_SAK_SED_FORESPØRSEL);

        ferdigstillJournalpost.utfør(prosessinstans);

        verify(tpsFasade, never()).hentIdentForAktørId(anyString());
        verify(joarkFasade, never()).oppdaterJournalpost(anyString(), any(JournalpostOppdatering.class), anyBoolean());
    }

    private static Prosessinstans hentProsessinstans() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, JOURNALPOST_ID);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, AKTØR_ID);
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, GSAK_SAKSNUMMER);
        prosessinstans.setData(ProsessDataKey.HOVEDDOKUMENT_TITTEL, TITTEL);
        prosessinstans.setData(ProsessDataKey.DOKUMENT_ID, DOKUMENT_ID);
        prosessinstans.setBehandling(new Behandling());
        return prosessinstans;
    }


}