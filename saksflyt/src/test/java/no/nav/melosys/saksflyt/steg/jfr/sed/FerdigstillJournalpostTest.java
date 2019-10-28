package no.nav.melosys.saksflyt.steg.jfr.sed;

import no.nav.melosys.domain.*;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

        verify(joarkFasade).oppdaterJournalpost(eq(JOURNALPOST_ID), eq(BRUKER_ID), eq(GSAK_SAKSNUMMER), eq(TITTEL), eq(true));
        verify(tpsFasade).hentIdentForAktørId(eq(AKTØR_ID));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.hentFørsteProsessStegForType(prosessinstans.getType()));
    }

    @Test
    public void utfør_ikkeProsesstypeJournalfør_skalIkkeBehandlesVidere() throws Exception {

        Prosessinstans prosessinstans = hentProsessinstans();
        prosessinstans.setType(ProsessType.MOTTAK_SED_JOURNALFØRING);

        when(tpsFasade.hentIdentForAktørId(eq(AKTØR_ID))).thenReturn(BRUKER_ID);
        ferdigstillJournalpost.utfør(prosessinstans);

        verify(joarkFasade).oppdaterJournalpost(eq(JOURNALPOST_ID), eq(BRUKER_ID), eq(GSAK_SAKSNUMMER), eq(TITTEL), eq(true));
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FERDIG);
    }

    private Prosessinstans hentProsessinstans() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, JOURNALPOST_ID);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, AKTØR_ID);
        prosessinstans.setData(ProsessDataKey.GSAK_SAK_ID, GSAK_SAKSNUMMER);
        prosessinstans.setData(ProsessDataKey.HOVEDDOKUMENT_TITTEL, TITTEL);
        prosessinstans.setBehandling(new Behandling());
        return prosessinstans;
    }


}