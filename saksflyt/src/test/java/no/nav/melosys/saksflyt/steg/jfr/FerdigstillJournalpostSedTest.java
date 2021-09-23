package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.joark.JournalpostOppdatering;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FerdigstillJournalpostSedTest {
    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private PersondataFasade persondataFasade;

    private FerdigstillJournalpostSed ferdigstillJournalpostSed;

    private static final String JOURNALPOST_ID = "jp123";
    private static final String BRUKER_ID = "bruker123";
    private static final String AKTØR_ID = "aktør123";
    private static final String SAKSNUMMER = "MEL-1223";
    private static final String TITTEL = "tittel123";

    @BeforeEach
    public void setUp() {
        ferdigstillJournalpostSed = new FerdigstillJournalpostSed(joarkFasade, persondataFasade);
        when(persondataFasade.hentFolkeregisterident(AKTØR_ID)).thenReturn(BRUKER_ID);
    }

    @Test
    void utfør() throws Exception {
        Prosessinstans prosessinstans = hentProsessinstans();
        ferdigstillJournalpostSed.utfør(prosessinstans);

        JournalpostOppdatering forventetOppdatering = new JournalpostOppdatering.Builder()
            .medBrukerID(BRUKER_ID).medSaksnummer(SAKSNUMMER).medTittel(TITTEL).build();
        verify(joarkFasade).oppdaterJournalpost(JOURNALPOST_ID, forventetOppdatering, true);
    }


    private static Prosessinstans hentProsessinstans() {

        Aktoer bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);
        bruker.setAktørId(AKTØR_ID);

        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(123L);
        fagsak.setSaksnummer(SAKSNUMMER);
        fagsak.getAktører().add(bruker);

        Behandling behandling = new Behandling();
        behandling.setTema(Behandlingstema.UTSENDT_ARBEIDSTAKER);
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
