package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.joark.JournalpostOppdatering;
import no.nav.melosys.integrasjon.tps.TpsFasade;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FerdigstillJournalpostSedTest {
    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private TpsFasade tpsFasade;

    private FerdigstillJournalpostSed ferdigstillJournalpostSed;

    private static final String JOURNALPOST_ID = "jp123";
    private static final String BRUKER_ID = "bruker123";
    private static final String AKTØR_ID = "aktør123";
    private static final Long GSAK_SAKSNUMMER = 123L;
    private static final String TITTEL = "tittel123";
    private static final String DOKUMENT_ID = "dokID123";

    @Before
    public void setUp() throws IkkeFunnetException {
        ferdigstillJournalpostSed = new FerdigstillJournalpostSed(joarkFasade, tpsFasade);
        when(tpsFasade.hentIdentForAktørId(eq(AKTØR_ID))).thenReturn(BRUKER_ID);
    }

    @Test
    public void utfør() throws Exception {
        Prosessinstans prosessinstans = hentProsessinstans();
        ferdigstillJournalpostSed.utfør(prosessinstans);

        JournalpostOppdatering forventetOppdatering = new JournalpostOppdatering.Builder()
            .medBrukerID(BRUKER_ID).medArkivSakID(GSAK_SAKSNUMMER).medTittel(TITTEL).build();
        verify(joarkFasade).oppdaterJournalpost(eq(JOURNALPOST_ID), eq(forventetOppdatering), eq(true));
    }


    private static Prosessinstans hentProsessinstans() {

        Aktoer bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        bruker.setAktørId(AKTØR_ID);

        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(123L);
        fagsak.getAktører().add(bruker);

        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);

        MelosysEessiMelding eessiMelding = new MelosysEessiMelding();
        eessiMelding.setJournalpostId(JOURNALPOST_ID);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, eessiMelding);
        prosessinstans.setData(ProsessDataKey.HOVEDDOKUMENT_TITTEL, TITTEL);
        prosessinstans.setBehandling(behandling);

        return prosessinstans;
    }
}