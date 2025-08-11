package no.nav.melosys.saksflyt.steg.brev;

import java.util.Optional;

import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.adresse.StrukturertAdresse;
import no.nav.melosys.domain.arkiv.Distribusjonstype;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.saksflyt.TestdataFactory;
import no.nav.melosys.saksflytapi.domain.ProsessDataKey;
import no.nav.melosys.saksflytapi.domain.ProsessStatus;
import no.nav.melosys.saksflytapi.domain.ProsessType;
import no.nav.melosys.saksflytapi.domain.Prosessinstans;
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
        assertThrows(FunksjonellException.class, () -> distribuerJournalpost.utfør(Prosessinstans.builder().medType(ProsessType.OPPRETT_SAK).medStatus(ProsessStatus.KLAR).build()));
    }

    @Test
    void utførFeilerVedManglendeJournalpostId() {
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        Prosessinstans prosessinstans = Prosessinstans.builder()
            .medType(ProsessType.OPPRETT_SAK)
            .medStatus(ProsessStatus.KLAR)
            .medBehandling(behandling)
            .medData(ProsessDataKey.BREVBESTILLING, new DokgenBrevbestilling.Builder<>().build())
            .build();

        assertThrows(FunksjonellException.class, () -> distribuerJournalpost.utfør(prosessinstans));
    }

    @Test
    void utførFeilerVedManglendeMottaker() {
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        Prosessinstans prosessinstans = Prosessinstans.builder()
            .medType(ProsessType.OPPRETT_SAK)
            .medStatus(ProsessStatus.KLAR)
            .medBehandling(behandling)
            .medData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID, "123")
            .medData(ProsessDataKey.BREVBESTILLING, new DokgenBrevbestilling.Builder<>().build())
            .build();
        assertThrows(FunksjonellException.class, () -> distribuerJournalpost.utfør(prosessinstans));
    }

    @Test
    void utførDistribuerJournalpostUtenAdresse() {
        String journalpostId = "12345";
        Prosessinstans prosessinstans = setupHappypath(journalpostId, Mottakerroller.BRUKER, Distribusjonstype.VIKTIG);

        distribuerJournalpost.utfør(prosessinstans);

        verify(mockDoksysFasade).distribuerJournalpost(journalpostId, Distribusjonstype.VIKTIG);
    }

    @Test
    void utførDistribuerJournalpostMedPostadresse() {
        String journalpostId = "12345";

        Prosessinstans prosessinstans = setupHappypath(journalpostId, Mottakerroller.FULLMEKTIG, Distribusjonstype.ANNET);
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.ORGNR, "123456789")
            .build();

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
        Prosessinstans prosessinstans = setupHappypath(journalpostId, Mottakerroller.FULLMEKTIG, Distribusjonstype.VEDTAK);
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.ORGNR, "123456789")
            .build();

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
        Prosessinstans prosessinstans = setupHappypath(journalpostId, Mottakerroller.FULLMEKTIG, Distribusjonstype.ANNET);
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.AKTØR_ID, "12345678901")
            .build();

        distribuerJournalpost.utfør(prosessinstans);

        verify(mockDoksysFasade).distribuerJournalpost(journalpostId, Distribusjonstype.ANNET);
    }

    @Test
    void utførDistribuerJournalpostMedUtenlandskMyndighet() {
        final String journalpostId = "12345";
        final String institusjonID = "GB:A100";
        Prosessinstans prosessinstans = setupHappypath(journalpostId, Mottakerroller.UTENLANDSK_TRYGDEMYNDIGHET, Distribusjonstype.VIKTIG);
        prosessinstans = prosessinstans.toBuilder()
            .medData(ProsessDataKey.INSTITUSJON_ID, institusjonID)
            .build();

        var utenlandskMyndighet = new UtenlandskMyndighet();
        utenlandskMyndighet.setLandkode(Land_iso2.GB);

        when(mockUtenlandskMyndighetService.hentUtenlandskMyndighet(eq(Land_iso2.GB), any())).thenReturn(utenlandskMyndighet);

        distribuerJournalpost.utfør(prosessinstans);

        verify(mockDoksysFasade).distribuerJournalpost(eq(journalpostId), any(StrukturertAdresse.class), eq(Distribusjonstype.VIKTIG));
    }

    private Prosessinstans setupHappypath(String journalpostId, Mottakerroller rolle, Distribusjonstype distribusjonstype) {
        Behandling behandling = TestdataFactory.lagBehandling();
        DokgenBrevbestilling brevbestilling = new MangelbrevBrevbestilling.Builder()
            .medDistribusjonstype(distribusjonstype)
            .build();

        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);

        return Prosessinstans.builder()
            .medType(ProsessType.OPPRETT_SAK)
            .medStatus(ProsessStatus.KLAR)
            .medBehandling(behandling)
            .medData(ProsessDataKey.DISTRIBUERBAR_JOURNALPOST_ID, journalpostId)
            .medData(ProsessDataKey.BREVBESTILLING, brevbestilling)
            .medData(ProsessDataKey.MOTTAKER, rolle)
            .build();
    }
}
