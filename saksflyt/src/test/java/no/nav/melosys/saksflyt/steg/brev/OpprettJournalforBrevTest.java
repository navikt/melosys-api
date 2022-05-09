package no.nav.melosys.saksflyt.steg.brev;

import java.util.List;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.arkiv.*;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.FritekstbrevBrevbestilling;
import no.nav.melosys.domain.brev.InnvilgelseBrevbestilling;
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.TestdataFactory;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.brev.DokumentNavnService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.DokumentHentingService;
import no.nav.melosys.service.dokument.brev.mapper.DokumentproduksjonsInfoMapper;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpprettJournalforBrevTest {

    @Mock
    private BehandlingService mockBehandlingService;
    @Mock
    private DokgenService mockDokgenService;
    @Mock
    private UtenlandskMyndighetService mockUtenlandskMyndighetService;
    @Mock
    private JoarkFasade mockJoarkFasade;
    @Mock
    private EregFasade mockEregFasade;
    @Mock
    private PersondataFasade mockPersondataFasade;
    @Mock
    private DokumentNavnService mockDokumentNavnService;
    @Mock
    private DokumentHentingService mockDokumentHentingService;
    @Captor
    ArgumentCaptor<OpprettJournalpost> opprettJournalpostCaptor;

    private OpprettJournalforBrev opprettJournalforBrev;

    @BeforeEach
    void init() {
        opprettJournalforBrev = new OpprettJournalforBrev(mockBehandlingService, mockDokgenService,
            mockUtenlandskMyndighetService, mockJoarkFasade, mockPersondataFasade, mockEregFasade,
            mockDokumentNavnService, mockDokumentHentingService);
    }

    @Test
    void utførFeilerVedManglendeBehandling() {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, "12345678901");
        assertThatThrownBy(() -> opprettJournalforBrev.utfør(prosessinstans))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Prosessinstans mangler behandling");
    }

    @Test
    void utførFeilerVedManglendeMottaker() {
        Prosessinstans prosessinstans = new Prosessinstans();
        assertThatThrownBy(() -> opprettJournalforBrev.utfør(prosessinstans))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Mangler mottaker");
    }

    @Test
    void utførOpprettJournalforBrevTilBruker() {
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(mockJoarkFasade.opprettJournalpost(any(), anyBoolean())).thenReturn("12234");
        when(mockDokgenService.hentDokumentInfo(any())).thenReturn(TestdataFactory.lagDokumentInfo());

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .build();

        Prosessinstans prosessinstans = lagProsessinstans(behandling, brevbestilling);

        opprettJournalforBrev.utfør(prosessinstans);

        verify(mockBehandlingService).hentBehandlingMedSaksopplysninger(anyLong());
        verify(mockDokgenService).produserBrev(any(Aktoer.class), any(DokgenBrevbestilling.class), eq(false));
        verify(mockJoarkFasade).opprettJournalpost(any(), anyBoolean());
    }

    @Test
    void utførOpprettJournalforBrevTilRepresentant() {
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(mockDokgenService.hentDokumentInfo(any())).thenReturn(TestdataFactory.lagDokumentInfo());
        when(mockJoarkFasade.opprettJournalpost(any(), anyBoolean())).thenReturn("12234");
        when(mockEregFasade.hentOrganisasjonNavn(any())).thenReturn("Advokatene AS");

        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.REPRESENTANT);
        mottaker.setOrgnr("987654321");

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .build();

        Prosessinstans prosessinstans = lagProsessinstansMedOrgnr(behandling, mottaker, brevbestilling);

        opprettJournalforBrev.utfør(prosessinstans);

        verify(mockBehandlingService).hentBehandlingMedSaksopplysninger(anyLong());
        verify(mockDokgenService).produserBrev(any(Aktoer.class), any(DokgenBrevbestilling.class), eq(false));
        verify(mockJoarkFasade).opprettJournalpost(any(), anyBoolean());
    }

    @Test
    void utfør_StorbritanniaInnvilgelseForBruker_korrektTittel() {
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(mockJoarkFasade.opprettJournalpost(any(), anyBoolean())).thenReturn("12234");
        DokumentproduksjonsInfoMapper dokumentproduksjonsInfoMapper = new DokumentproduksjonsInfoMapper(new FakeUnleash());
        when(mockDokgenService.hentDokumentInfo(any())).thenReturn(dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(STORBRITANNIA));
        Aktoer mottaker = lagMottaker("12234");

        InnvilgelseBrevbestilling brevbestilling = new InnvilgelseBrevbestilling.Builder()
            .medBehandlingId(1L)
            .medProduserbartdokument(STORBRITANNIA)
            .build();
        Prosessinstans prosessinstans = lagProsessinstansMedMottaker(behandling, mottaker, brevbestilling);

        when(mockDokumentNavnService.utledDokumentNavn(behandling, dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(STORBRITANNIA), mottaker)).thenReturn("Vedtak om medlemskap, Attest for utsendt arbeidstaker");

        opprettJournalforBrev.utfør(prosessinstans);

        verify(mockJoarkFasade).opprettJournalpost(opprettJournalpostCaptor.capture(), anyBoolean());

        OpprettJournalpost captured = opprettJournalpostCaptor.getValue();
        assertThat(captured.getHoveddokument().getTittel()).isEqualTo("Vedtak om medlemskap, Attest for utsendt arbeidstaker");
    }

    @Test
    void utførOpprettJournalforMangelbrevTilBruker() {
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(mockJoarkFasade.opprettJournalpost(any(), anyBoolean())).thenReturn("12234");
        when(mockDokgenService.hentDokumentInfo(any())).thenReturn(TestdataFactory.lagDokumentInfo());

        MangelbrevBrevbestilling brevbestilling = new MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(MANGELBREV_BRUKER)
            .medManglerInfoFritekst("Mangler")
            .build();

        Prosessinstans prosessinstans = lagProsessinstans(behandling, brevbestilling);

        opprettJournalforBrev.utfør(prosessinstans);

        verify(mockBehandlingService).hentBehandlingMedSaksopplysninger(anyLong());
        verify(mockDokgenService).produserBrev(any(Aktoer.class), any(MangelbrevBrevbestilling.class), eq(false));
        verify(mockJoarkFasade).opprettJournalpost(any(), anyBoolean());
    }

    @Test
    void utførOpprettJournalforMangelbrevKopiTilBruker() {
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(mockJoarkFasade.opprettJournalpost(any(), anyBoolean())).thenReturn("12234");
        when(mockDokgenService.hentDokumentInfo(any())).thenReturn(TestdataFactory.lagDokumentInfo());

        MangelbrevBrevbestilling brevbestilling = new MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(MANGELBREV_BRUKER)
            .medManglerInfoFritekst("Mangler")
            .medBestillKopi(true)
            .build();

        Prosessinstans prosessinstans = lagProsessinstans(behandling, brevbestilling);

        opprettJournalforBrev.utfør(prosessinstans);

        verify(mockBehandlingService).hentBehandlingMedSaksopplysninger(anyLong());
        //noinspection ConstantConditions - brevbestilling er ikke null
        verify(mockDokgenService).produserBrev(any(Aktoer.class), refEq(brevbestilling), eq(false));
        verify(mockJoarkFasade).opprettJournalpost(any(), anyBoolean());
    }

    @Test
    void utførOpprettJournalforFritekstbrev_feilerUtentittel() {
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);

        FritekstbrevBrevbestilling brevbestilling = new FritekstbrevBrevbestilling.Builder()
            .medProduserbartdokument(GENERELT_FRITEKSTBREV_BRUKER)
            .build();

        Prosessinstans prosessinstans = lagProsessinstans(behandling, brevbestilling);

        assertThatThrownBy(() -> opprettJournalforBrev.utfør(prosessinstans))
            .isInstanceOf(FunksjonellException.class)
            .hasMessageContaining("Tittel til fritekstbrev mangler");
    }

    @Test
    void utførOpprettJournalforFritekstbrev() {
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(mockJoarkFasade.opprettJournalpost(any(), anyBoolean())).thenReturn("12234");
        when(mockDokgenService.hentDokumentInfo(any())).thenReturn(TestdataFactory.lagDokumentInfo());

        FritekstbrevBrevbestilling brevbestilling = new FritekstbrevBrevbestilling.Builder()
            .medProduserbartdokument(GENERELT_FRITEKSTBREV_BRUKER)
            .medFritekstTittel("Tittel")
            .medFritekst("Innhold")
            .build();

        Prosessinstans prosessinstans = lagProsessinstans(behandling, brevbestilling);

        opprettJournalforBrev.utfør(prosessinstans);

        verify(mockBehandlingService).hentBehandlingMedSaksopplysninger(anyLong());
        //noinspection ConstantConditions - brevbestilling er ikke null
        verify(mockDokgenService).produserBrev(any(Aktoer.class), refEq(brevbestilling), eq(false));
        verify(mockJoarkFasade).opprettJournalpost(any(), anyBoolean());
    }

    @Test
    void utfør_feilerMedIkkeFunnetException_NårJournalpostIkkeFinnesForVedleggsdokument() {
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(mockDokgenService.hentDokumentInfo(any())).thenReturn(TestdataFactory.lagDokumentInfo());

        List<SaksvedleggBestilling> saksvedleggBestillingList = List.of(new SaksvedleggBestilling("1", "2"));
        FritekstbrevBrevbestilling brevbestilling = new FritekstbrevBrevbestilling.Builder()
            .medProduserbartdokument(GENERELT_FRITEKSTBREV_BRUKER)
            .medFritekstTittel("Tittel")
            .medSaksvedleggBestilling(saksvedleggBestillingList)
            .build();

        Prosessinstans prosessinstans = lagProsessinstans(behandling, brevbestilling);


        assertThatThrownBy(() -> opprettJournalforBrev.utfør(prosessinstans))
            .isInstanceOf(IkkeFunnetException.class)
            .hasMessage("Finner ikke journalpost 1 for saken saksnummer");
    }

    @Test
    void utfør_henterVedleggDokumenterFraJoark() {
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(mockJoarkFasade.opprettJournalpost(any(), anyBoolean())).thenReturn("12234");
        when(mockDokgenService.hentDokumentInfo(any())).thenReturn(TestdataFactory.lagDokumentInfo());
        when(mockDokumentHentingService.hentDokumenter("MEL-test")).thenReturn(
            List.of(lagJournalpost("1", "2", "tittel 1"),
                lagJournalpost("3", "4", "tittel 2")
            )
        );
        when(mockJoarkFasade.hentDokument("1", "2")).thenReturn(new byte[]{1, 2});
        when(mockJoarkFasade.hentDokument("3", "4")).thenReturn(new byte[]{3, 4});

        List<SaksvedleggBestilling> saksvedleggBestillingList =
            List.of(new SaksvedleggBestilling("1", "2"), new SaksvedleggBestilling("3", "4"));
        FritekstbrevBrevbestilling brevbestilling = new FritekstbrevBrevbestilling.Builder()
            .medProduserbartdokument(GENERELT_FRITEKSTBREV_BRUKER)
            .medFritekstTittel("Tittel")
            .medSaksvedleggBestilling(saksvedleggBestillingList)
            .medFritekst("Innhold")
            .build();

        Prosessinstans prosessinstans = lagProsessinstans(behandling, brevbestilling);

        opprettJournalforBrev.utfør(prosessinstans);

        verify(mockDokumentHentingService).hentDokumenter("MEL-test");
        verify(mockJoarkFasade).hentDokument("1", "2");
        verify(mockJoarkFasade).hentDokument("3", "4");
        verify(mockJoarkFasade).opprettJournalpost(opprettJournalpostCaptor.capture(), anyBoolean());

        OpprettJournalpost captured = opprettJournalpostCaptor.getValue();
        assertThat(captured.getHoveddokument().getTittel()).isEqualTo("Tittel");
        assertThat(captured.getVedlegg())
            .extracting(fysiskDokument -> fysiskDokument.getDokumentVarianter()
                    .stream().map(DokumentVariant::getData).findFirst().orElse(null),
                ArkivDokument::getTittel)
            .containsExactly(Tuple.tuple(new byte[]{1, 2}, "tittel 1"),
                Tuple.tuple(new byte[]{3, 4}, "tittel 2"));
    }

    private Prosessinstans lagProsessinstans(Behandling behandling, DokgenBrevbestilling brevbestilling) {
        Aktoer mottaker = lagMottaker("1234");

        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.BREVBESTILLING, brevbestilling);
        prosessinstans.setData(ProsessDataKey.MOTTAKER, mottaker.getRolle());
        prosessinstans.setData(ProsessDataKey.AKTØR_ID, mottaker.getAktørId());
        return prosessinstans;
    }

    private Prosessinstans lagProsessinstansMedMottaker(Behandling behandling, Aktoer mottaker, DokgenBrevbestilling brevbestilling) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.BREVBESTILLING, brevbestilling);
        prosessinstans.setData(ProsessDataKey.MOTTAKER, mottaker.getRolle());
        if (mottaker.getAktørId() != null) prosessinstans.setData(ProsessDataKey.AKTØR_ID, mottaker.getAktørId());
        if (mottaker.getOrgnr() != null) prosessinstans.setData(ProsessDataKey.ORGNR, mottaker.getOrgnr());
        if (mottaker.getInstitusjonId() != null)
            prosessinstans.setData(ProsessDataKey.INSTITUSJON_ID, mottaker.getInstitusjonId());
        return prosessinstans;
    }

    private Prosessinstans lagProsessinstansMedOrgnr(Behandling behandling, Aktoer mottaker, DokgenBrevbestilling brevbestilling) {
        Prosessinstans prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setData(ProsessDataKey.BREVBESTILLING, brevbestilling);
        prosessinstans.setData(ProsessDataKey.MOTTAKER, mottaker.getRolle());
        prosessinstans.setData(ProsessDataKey.ORGNR, mottaker.getOrgnr());
        return prosessinstans;
    }

    private static Aktoer lagMottaker(String aktørID) {
        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.BRUKER);
        mottaker.setAktørId(aktørID);
        mottaker.setOrgnr(null);
        mottaker.setInstitusjonId(null);
        return mottaker;
    }

    private static Journalpost lagJournalpost(String journalpostId, String dokumentId, String tittel) {
        Journalpost journalpost = new Journalpost(journalpostId);
        journalpost.setJournalposttype(Journalposttype.UT);
        journalpost.setAvsenderId("nav");
        journalpost.setAvsenderId("NAVAT:07");
        journalpost.setKorrespondansepartNavn("Test12345");
        ArkivDokument arkivDokument = new ArkivDokument();
        arkivDokument.setDokumentId(dokumentId);
        arkivDokument.setTittel(tittel);

        journalpost.setHoveddokument(arkivDokument);

        return journalpost;
    }
}
