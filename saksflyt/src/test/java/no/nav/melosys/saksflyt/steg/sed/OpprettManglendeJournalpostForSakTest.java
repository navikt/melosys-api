package no.nav.melosys.saksflyt.steg.sed;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.eessi.melding.MelosysEessiMelding;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.dokument.sed.EessiService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OpprettManglendeJournalpostForSakTest {
    @Mock
    private EessiService eessiService;
    @Mock
    private JoarkFasade joarkFasade;

    private final FakeUnleash fakeUnleash = new FakeUnleash();

    private OpprettManglendeJournalpostForSak opprettManglendeJournalpostForSak;

    private static final String JOURNALPOST_ID = "123";

    @BeforeEach
    public void setup() {
        fakeUnleash.enableAll();
        opprettManglendeJournalpostForSak = new OpprettManglendeJournalpostForSak(joarkFasade, eessiService, fakeUnleash);
    }

    @Test
    void utfør_journalpostErFraEessi_verifiserOppdaterSaksrelasjon() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.JOURNALPOST_ID, JOURNALPOST_ID);

        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(123L);
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        prosessinstans.setBehandling(behandling);

        Journalpost journalpost = new Journalpost(JOURNALPOST_ID);
        journalpost.setMottaksKanal("EESSI");
        when(joarkFasade.hentJournalpost(JOURNALPOST_ID)).thenReturn(journalpost);

        MelosysEessiMelding melosysEessiMelding = new MelosysEessiMelding();
        melosysEessiMelding.setBucType("LA_BUC_04");
        melosysEessiMelding.setRinaSaksnummer("321323");
        when(eessiService.hentSedTilknyttetJournalpost(JOURNALPOST_ID)).thenReturn(melosysEessiMelding);

        opprettManglendeJournalpostForSak.utfør(prosessinstans);

        verify(eessiService).opprettJournalpostForTidligereSed(
            melosysEessiMelding.getRinaSaksnummer()
        );
    }
}
