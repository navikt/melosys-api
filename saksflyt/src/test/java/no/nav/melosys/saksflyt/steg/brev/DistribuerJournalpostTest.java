package no.nav.melosys.saksflyt.steg.brev;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.arkiv.Distribusjonstype;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.domain.util.Land_ISO2;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.saksflyt.TestdataFactory;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
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
    private UtenlandskMyndighetService mockUtenlandskMyndighetService;
    @Mock
    private KodeverkService mockKodeverkService;

    private DistribuerJournalpost distribuerJournalpost;

    @BeforeEach
    void init() {
        distribuerJournalpost = new DistribuerJournalpost(mockDoksysFasade, mockEregFasade,
            mockKontaktopplysningService, mockBehandlingService, mockUtenlandskMyndighetService, mockKodeverkService);
    }

    @Test
    void utførFeilerVedManglendeBehandling() {
        assertThrows(FunksjonellException.class, () -> distribuerJournalpost.utfør(new Prosessinstans()));
    }

    @Test
    void utførFeilerVedManglendeJournalpostId() {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.BREVBESTILLING, new DokgenBrevbestilling.Builder<>().build());

        assertThrows(FunksjonellException.class, () -> distribuerJournalpost.utfør(prosessinstans));
    }

    @Test
    void utførFeilerVedManglendeMottaker() {
        Prosessinstans prosessinstans = new Prosessinstans();
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID, "123");
        prosessinstans.setData(ProsessDataKey.BREVBESTILLING, new DokgenBrevbestilling.Builder<>().build());
        assertThrows(FunksjonellException.class, () -> distribuerJournalpost.utfør(prosessinstans));
    }

    @Test
    void utførDistribuerJournalpostUtenAdresse() {
        String journalpostId = "12345";
        Prosessinstans prosessinstans = setupHappypath(journalpostId, Aktoersroller.BRUKER, Distribusjonstype.VIKTIG);

        distribuerJournalpost.utfør(prosessinstans);

        verify(mockDoksysFasade).distribuerJournalpost(journalpostId, Distribusjonstype.VIKTIG);
    }

    @Test
    void utførDistribuerJournalpostMedPostadresse() {
        String journalpostId = "12345";

        Prosessinstans prosessinstans = setupHappypath(journalpostId, Aktoersroller.REPRESENTANT, Distribusjonstype.ANNET);
        prosessinstans.setData(ProsessDataKey.ORGNR, "123456789");

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(TestdataFactory.lagOrgMedPostadresse());

        when(mockEregFasade.hentOrganisasjon(any())).thenReturn(saksopplysning);
        when(mockKontaktopplysningService.hentKontaktopplysning(any(), any())).thenReturn(Optional.of(TestdataFactory.lagKontaktOpplysning()));

        distribuerJournalpost.utfør(prosessinstans);

        verify(mockDoksysFasade).distribuerJournalpost(eq(journalpostId), any(StrukturertAdresse.class), any(), any(), eq(Distribusjonstype.ANNET));
    }

    @Test
    void utførDistribuerJournalpostMedForretningsadresse() {
        String journalpostId = "12345";
        Prosessinstans prosessinstans = setupHappypath(journalpostId, Aktoersroller.REPRESENTANT, Distribusjonstype.VEDTAK);
        prosessinstans.setData(ProsessDataKey.ORGNR, "123456789");

        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(TestdataFactory.lagOrgMedForretningsadresse());

        when(mockEregFasade.hentOrganisasjon(any())).thenReturn(saksopplysning);
        when(mockKontaktopplysningService.hentKontaktopplysning(any(), any())).thenReturn(Optional.of(TestdataFactory.lagKontaktOpplysning()));
        when(mockKodeverkService.dekod(any(), any())).thenReturn("Andeby");

        distribuerJournalpost.utfør(prosessinstans);

        verify(mockDoksysFasade).distribuerJournalpost(eq(journalpostId), any(StrukturertAdresse.class), any(), any(), eq(Distribusjonstype.VEDTAK));
    }

    @Test
    void utførDistribuerJournalpostMedReperesentantPerson() {
        String journalpostId = "12345";
        Prosessinstans prosessinstans = setupHappypath(journalpostId, Aktoersroller.REPRESENTANT, Distribusjonstype.ANNET);
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, "12345678901");

        distribuerJournalpost.utfør(prosessinstans);

        verify(mockDoksysFasade).distribuerJournalpost(journalpostId, Distribusjonstype.ANNET);
    }

    @Test
    void utførDistribuerJournalpostMedUtenlandskMyndighet() {
        final String journalpostId = "12345";
        final String institusjonId = "GB:A100";
        Prosessinstans prosessinstans = setupHappypath(journalpostId, Aktoersroller.TRYGDEMYNDIGHET, Distribusjonstype.VIKTIG);
        prosessinstans.setData(ProsessDataKey.INSTITUSJON_ID, institusjonId);

        var utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.landkode = Land_ISO2.GB;

        when(mockUtenlandskMyndighetService.hentUtenlandskMyndighetForInstitusjonID(eq(institusjonId))).thenReturn(utenlandskMyndighet);

        distribuerJournalpost.utfør(prosessinstans);

        verify(mockDoksysFasade).distribuerJournalpost(eq(journalpostId), any(StrukturertAdresse.class), eq(Distribusjonstype.VIKTIG));
    }

    private Prosessinstans setupHappypath(String journalpostId, Aktoersroller rolle, Distribusjonstype distribusjonstype) {
        Behandling behandling = TestdataFactory.lagBehandling();
        Prosessinstans prosessinstans = new Prosessinstans();
        DokgenBrevbestilling brevbestilling = new MangelbrevBrevbestilling.Builder()
            .medDistribusjonstype(distribusjonstype)
            .build();

        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID, journalpostId);
        prosessinstans.setData(ProsessDataKey.BREVBESTILLING, brevbestilling);
        prosessinstans.setData(ProsessDataKey.MOTTAKER, rolle);

        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);

        return prosessinstans;
    }
}
