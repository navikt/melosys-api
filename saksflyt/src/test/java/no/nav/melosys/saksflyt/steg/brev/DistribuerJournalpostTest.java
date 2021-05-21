package no.nav.melosys.saksflyt.steg.brev;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;
import no.nav.melosys.domain.dokument.adresse.StrukturertAdresse;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.saksflyt.TestdataFactory;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.kodeverk.KodeverkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistribuerJournalpostTest {

    @Mock
    private DoksysFasade mockDoksysFasade;
    @Mock
    private EregFasade mockEregFasade;
    @Mock
    private KontaktopplysningService mockKontaktopplysningService;
    @Mock
    private BehandlingService mockBehandlingService;
    @Mock
    private KodeverkService mockKodeverkService;

    private DistribuerJournalpost distribuerJournalpost;

    @BeforeEach
    void init() {
        distribuerJournalpost = new DistribuerJournalpost(mockDoksysFasade, mockEregFasade,
            mockKontaktopplysningService, mockBehandlingService, mockKodeverkService);
    }

    @Test
    void utførFeilerVedManglendeBehandling() {
        assertThrows(FunksjonellException.class, () -> distribuerJournalpost.utfør(new Prosessinstans()));
    }

    @Test
    void utførFeilerVedManglendeJournalpostId() throws Exception {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.BREVBESTILLING, new DokgenBrevbestilling());

        assertThrows(FunksjonellException.class, () -> distribuerJournalpost.utfør(prosessinstans));
    }

    @Test
    void utførFeilerVedManglendeMottaker() throws Exception {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID, "123");
        prosessinstans.setData(ProsessDataKey.BREVBESTILLING, new DokgenBrevbestilling());
        assertThrows(FunksjonellException.class, () -> distribuerJournalpost.utfør(prosessinstans));
    }

    @Test
    void utførDistribuerJournalpostUtenAdresse() throws Exception {
        String journalpostId = "12345";
        Prosessinstans prosessinstans = setupHappypath(journalpostId, Aktoersroller.BRUKER);

        distribuerJournalpost.utfør(prosessinstans);

        verify(mockDoksysFasade).distribuerJournalpost(eq(journalpostId));
    }

    @Test
    void utførDistribuerJournalpostMedPostadresse() throws Exception {
        String journalpostId = "12345";

        Prosessinstans prosessinstans = setupHappypath(journalpostId, Aktoersroller.REPRESENTANT);

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(TestdataFactory.lagOrgMedPostadresse());

        when(mockEregFasade.hentOrganisasjon(any())).thenReturn(saksopplysning);
        when(mockKontaktopplysningService.hentKontaktopplysning(any(), any())).thenReturn(Optional.of(TestdataFactory.lagKontaktOpplysning()));

        distribuerJournalpost.utfør(prosessinstans);

        verify(mockDoksysFasade).distribuerJournalpost(eq(journalpostId), any(StrukturertAdresse.class), any(), any());
    }

    @Test
    void utførDistribuerJournalpostMedForretningsadresse() throws Exception {
        String journalpostId = "12345";
        Prosessinstans prosessinstans = setupHappypath(journalpostId, Aktoersroller.REPRESENTANT);

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(TestdataFactory.lagOrgMedForretningsadresse());

        when(mockEregFasade.hentOrganisasjon(any())).thenReturn(saksopplysning);
        when(mockKontaktopplysningService.hentKontaktopplysning(any(), any())).thenReturn(Optional.of(TestdataFactory.lagKontaktOpplysning()));
        when(mockKodeverkService.dekod(any(), any(), any())).thenReturn("Andeby");

        distribuerJournalpost.utfør(prosessinstans);

        verify(mockDoksysFasade).distribuerJournalpost(eq(journalpostId), any(StrukturertAdresse.class), any(), any());
    }

    private Prosessinstans setupHappypath(String journalpostId, Aktoersroller rolle) {
        Behandling behandling = TestdataFactory.lagBehandling();
        Prosessinstans prosessinstans = new Prosessinstans();
        DokgenBrevbestilling brevbestilling = new MangelbrevBrevbestilling.Builder()
            .build();

        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID, journalpostId);
        prosessinstans.setData(ProsessDataKey.BREVBESTILLING, brevbestilling);
        prosessinstans.setData(ProsessDataKey.MOTTAKER, rolle);

        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling);

        return prosessinstans;
    }
}
