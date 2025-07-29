package no.nav.melosys.saksflyt.steg.jfr;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.BehandlingTestFactory;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.FagsakTestFactory;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.Journalposttype;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.integrasjon.joark.JournalpostOppdatering;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
import no.nav.melosys.service.dokument.sed.EessiService;
import no.nav.melosys.service.oppgave.OppgaveFactory;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.FagsakTestFactory.BRUKER_AKTØR_ID;
import static no.nav.melosys.domain.FagsakTestFactory.SAKSNUMMER;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FerdigstillJournalpostSedTest {
    @Mock
    private JoarkFasade joarkFasade;
    @Mock
    private PersondataFasade persondataFasade;
    @Mock
    private EessiService eessiService;

    private FerdigstillJournalpostSed ferdigstillJournalpostSed;

    private static final String JOURNALPOST_ID = "jp123";
    private static final String BRUKER_ID = "bruker123";
    private static final String TITTEL = "tittel123";

    private final OppgaveFactory oppgaveFactory = new OppgaveFactory();

    @BeforeEach
    public void setUp() {
        ferdigstillJournalpostSed = new FerdigstillJournalpostSed(joarkFasade, persondataFasade, oppgaveFactory, eessiService);
    }

    @Test
    void utfør_oppdatererJournalpost_nårJournalpostIkkeErFerdigstilt() {
        when(persondataFasade.hentFolkeregisterident(BRUKER_AKTØR_ID)).thenReturn(BRUKER_ID);
        Journalpost journalpost = new Journalpost(JOURNALPOST_ID);
        journalpost.setErFerdigstilt(false);
        journalpost.setJournalposttype(Journalposttype.INN);
        when(joarkFasade.hentJournalpost(JOURNALPOST_ID)).thenReturn(journalpost);

        Prosessinstans prosessinstans = hentProsessinstans();
        ferdigstillJournalpostSed.utfør(prosessinstans);


        JournalpostOppdatering forventetOppdatering = new JournalpostOppdatering.Builder()
            .medBrukerID(BRUKER_ID).medSaksnummer(SAKSNUMMER).medTittel(TITTEL).build();
        verify(joarkFasade).oppdaterOgFerdigstillJournalpost(JOURNALPOST_ID, forventetOppdatering);
    }

    @Test
    void utfør_oppdatererJournalpost_nårJournalpostIkkeErFerdigstilt_brukFagsakTema() {
        when(persondataFasade.hentFolkeregisterident(BRUKER_AKTØR_ID)).thenReturn(BRUKER_ID);
        Journalpost journalpost = new Journalpost(JOURNALPOST_ID);
        journalpost.setErFerdigstilt(false);
        journalpost.setJournalposttype(Journalposttype.INN);
        when(joarkFasade.hentJournalpost(JOURNALPOST_ID)).thenReturn(journalpost);

        Prosessinstans prosessinstans = hentProsessinstans();
        ferdigstillJournalpostSed.utfør(prosessinstans);


        JournalpostOppdatering forventetOppdatering = new JournalpostOppdatering.Builder()
            .medBrukerID(BRUKER_ID).medSaksnummer(SAKSNUMMER).medTittel(TITTEL).build();
        verify(joarkFasade).oppdaterOgFerdigstillJournalpost(JOURNALPOST_ID, forventetOppdatering);
    }

    @Test
    void utfør_gjørIngenting_nårJournalpostErFerdigstilt() {
        Journalpost journalpost = new Journalpost(JOURNALPOST_ID);
        journalpost.setErFerdigstilt(true);
        journalpost.setJournalposttype(Journalposttype.INN);
        when(joarkFasade.hentJournalpost(JOURNALPOST_ID)).thenReturn(journalpost);


        Prosessinstans prosessinstans = hentProsessinstans();
        ferdigstillJournalpostSed.utfør(prosessinstans);

        verify(joarkFasade).hentJournalpost(JOURNALPOST_ID);
        verifyNoMoreInteractions(joarkFasade);
    }

    @Test
    void utfør_gjørIngenting_nårJournalpostErUtgått() {
        Journalpost journalpost = new Journalpost(JOURNALPOST_ID);
        journalpost.setErUtgått(true);
        journalpost.setJournalposttype(Journalposttype.INN);
        when(joarkFasade.hentJournalpost(JOURNALPOST_ID)).thenReturn(journalpost);


        Prosessinstans prosessinstans = hentProsessinstans();
        ferdigstillJournalpostSed.utfør(prosessinstans);

        verify(joarkFasade).hentJournalpost(JOURNALPOST_ID);
        verifyNoMoreInteractions(joarkFasade);
    }

    private static Prosessinstans hentProsessinstans() {
        Fagsak fagsak = FagsakTestFactory.builder()
            .medGsakSaksnummer()
            .tema(Sakstemaer.TRYGDEAVGIFT)
            .medBruker()
            .build();

        Behandling behandling = BehandlingTestFactory.builderWithDefaults()
            .medTema(Behandlingstema.UTSENDT_ARBEIDSTAKER)
            .medType(Behandlingstyper.FØRSTEGANG)
            .medFagsak(fagsak)
            .build();

        MelosysEessiMelding eessiMelding = new MelosysEessiMelding();
        eessiMelding.setJournalpostId(JOURNALPOST_ID);

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.EESSI_MELDING, eessiMelding);
        prosessinstans.setData(ProsessDataKey.HOVEDDOKUMENT_TITTEL, TITTEL);
        prosessinstans.setBehandling(behandling);

        return prosessinstans;
    }
}
