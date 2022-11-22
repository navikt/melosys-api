package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.arkiv.Distribusjonstype;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.SaksvedleggBestilling;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.FritekstbrevBrevbestilling;
import no.nav.melosys.domain.brev.FritekstvedleggBestilling;
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Representerer;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.DokgenConsumer;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.behandling.UtledMottaksdato;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.DokumentHentingService;
import no.nav.melosys.service.dokument.DokumentproduksjonsInfo;
import no.nav.melosys.service.dokument.brev.BrevbestillingRequest;
import no.nav.melosys.service.dokument.brev.FritekstvedleggDto;
import no.nav.melosys.service.dokument.brev.KopiMottaker;
import no.nav.melosys.service.dokument.brev.SaksvedleggDto;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.ldap.SaksbehandlerService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.assertj.core.groups.Tuple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Optional.of;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.service.dokument.DokgenTestData.*;
import static no.nav.melosys.service.persondata.PersonopplysningerObjectFactory.lagPersonopplysninger;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DokgenServiceTest {

    public static final String FNR = "99887766554";
    public static final String ORGNR = "987654321";

    @Mock
    private DokgenConsumer mockDokgenConsumer;
    @Mock
    private JoarkFasade mockJoarkFasade;
    @Mock
    private KodeverkService mockKodeverkService;
    @Mock
    private BehandlingService mockBehandlingsService;
    @Mock
    private EregFasade mockEregFasade;
    @Mock
    private PersondataFasade mockPersondataFasade;
    @Mock
    private KontaktopplysningService mockKontaktOpplysningService;
    @Mock
    private BehandlingsresultatService mockBehandlingsresultatService;
    @Mock
    private BrevmottakerService mockBrevMottakerService;
    @Mock
    private ProsessinstansService mockProsessinstansService;
    @Mock
    private SaksbehandlerService mockSaksbehandlerService;
    @Mock
    private UtenlandskMyndighetService mockUtenlandskMyndighetService;
    @Mock
    private InnvilgelseFtrlMapper mockInnvilgelseFtrlMapper;
    @Mock
    private TrygdeavtaleMapper mockTrygdeavtaleMapper;
    @Mock
    private DokumentHentingService mockDokumentHentingService;
    @Mock
    private UtledMottaksdato mockUtledMottaksdato;
    @Captor
    private ArgumentCaptor<DokgenBrevbestilling> brevbestillingCaptor;

    private final FakeUnleash unleash = new FakeUnleash();

    private DokgenService dokgenService;

    private final byte[] expectedPdf = "pdf".getBytes();

    @BeforeEach
    void init() {
        unleash.enable("melosys.behandle_alle_saker");
        DokgenMapperDatahenter dokgenMapperDatahenter = new DokgenMapperDatahenter(mockBehandlingsresultatService, mockEregFasade,
            mockPersondataFasade, mockDokumentHentingService, mockKodeverkService);

        dokgenService = new DokgenService(mockDokgenConsumer, new DokumentproduksjonsInfoMapper(unleash),
            mockJoarkFasade,
            new DokgenMalMapper(dokgenMapperDatahenter, mockInnvilgelseFtrlMapper, mockTrygdeavtaleMapper, unleash),
            mockBehandlingsService, mockEregFasade, mockKontaktOpplysningService,
            mockBrevMottakerService, mockProsessinstansService, mockSaksbehandlerService,
            mockUtenlandskMyndighetService, unleash, mockUtledMottaksdato);

        reset(mockDokgenConsumer);
    }

    @Test
    void produserBrevFeilerUtilgjengeligMal() {
        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(ATTEST_A1)
            .build();

        assertThatThrownBy(() -> dokgenService.produserBrev(new Aktoer(), brevbestilling))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("ProduserbartDokument ATTEST_A1 er ikke støttet");
    }

    @Test
    void produserBrevTilBrukerOk() {
        when(mockDokgenConsumer.lagPdf(anyString(), any(), eq(false), eq(false))).thenReturn(expectedPdf);
        when(mockJoarkFasade.hentJournalpost(any())).thenReturn(lagJournalpost());
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling());
        when(mockPersondataFasade.hentPerson(anyString())).thenReturn(lagPersonopplysninger());
        when(mockKodeverkService.dekod(FellesKodeverk.POSTNUMMER, "0123")).thenReturn("Aker");
        when(mockKodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, "NO")).thenReturn("Norge");

        Aktoer mottaker = lagBruker();

        MangelbrevBrevbestilling brevbestilling = new MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(MANGELBREV_BRUKER)
            .medBehandlingId(123)
            .build();


        byte[] pdfResponse = dokgenService.produserBrev(mottaker, brevbestilling);


        assertThat(pdfResponse).isNotNull().isEqualTo(expectedPdf);

        verify(mockDokgenConsumer).lagPdf(any(), any(), eq(false), eq(false));
        verifyNoInteractions(mockEregFasade);
        verifyNoInteractions(mockKontaktOpplysningService);
    }

    @Test
    void produserBrev_nyOpprettSakToggleEnabled_henterDatoFraUtledMottaksdato() {
        unleash.enable("melosys.ny_opprett_sak");
        when(mockDokgenConsumer.lagPdf(anyString(), any(), eq(false), eq(false))).thenReturn(expectedPdf);
        var journalpost = lagJournalpost();
        when(mockJoarkFasade.hentJournalpost("journalpostId")).thenReturn(journalpost);
        var behandling = lagBehandling();
        behandling.setInitierendeJournalpostId("journalpostId");
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(mockPersondataFasade.hentPerson(anyString())).thenReturn(lagPersonopplysninger());
        when(mockKodeverkService.dekod(FellesKodeverk.POSTNUMMER, "0123")).thenReturn("Aker");
        when(mockKodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, "NO")).thenReturn("Norge");

        Aktoer mottaker = lagBruker();

        MangelbrevBrevbestilling brevbestilling = new MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(MANGELBREV_BRUKER)
            .medBehandlingId(123)
            .build();


        byte[] pdfResponse = dokgenService.produserBrev(mottaker, brevbestilling);


        assertThat(pdfResponse).isNotNull().isEqualTo(expectedPdf);

        verify(mockDokgenConsumer).lagPdf(any(), any(), eq(false), eq(false));
        verify(mockUtledMottaksdato).getMottaksdato(behandling, journalpost);
        verifyNoInteractions(mockEregFasade);
        verifyNoInteractions(mockKontaktOpplysningService);
    }

    @Test
    void produserBrevTilRepresentantOrganisasjonOk() {
        when(mockDokgenConsumer.lagPdf(anyString(), any(), eq(false), eq(false))).thenReturn(expectedPdf);
        when(mockJoarkFasade.hentJournalpost(any())).thenReturn(lagJournalpost());
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling());
        when(mockPersondataFasade.hentPerson(anyString())).thenReturn(lagPersonopplysninger());
        when(mockEregFasade.hentOrganisasjon(any())).thenReturn(lagSaksopplysning());
        when(mockKontaktOpplysningService.hentKontaktopplysning(any(), any())).thenReturn(of(lagKontaktOpplysning()));

        Aktoer mottaker = lagRepresentant(ORGNR, Representerer.BRUKER);

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medBehandlingId(123)
            .build();


        byte[] pdfResponse = dokgenService.produserBrev(mottaker, brevbestilling);


        assertThat(pdfResponse).isNotNull().isEqualTo(expectedPdf);

        verify(mockDokgenConsumer).lagPdf(any(), any(), eq(false), eq(false));
        verify(mockEregFasade).hentOrganisasjon(any());
        verify(mockKontaktOpplysningService).hentKontaktopplysning(any(), any());
    }

    @Test
    void produserBrevTilRepresentantPersonOk() {
        when(mockDokgenConsumer.lagPdf(anyString(), any(), eq(false), eq(false))).thenReturn(expectedPdf);
        when(mockJoarkFasade.hentJournalpost(any())).thenReturn(lagJournalpost());
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling());
        when(mockPersondataFasade.hentPerson(anyString())).thenReturn(lagPersonopplysninger());
        when(mockKodeverkService.dekod(FellesKodeverk.POSTNUMMER, "0123")).thenReturn("Aker");
        when(mockKodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, "NO")).thenReturn("Norge");

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medBehandlingId(123)
            .build();

        byte[] pdfResponse = dokgenService.produserBrev(lagRepresentant(FNR, Representerer.BRUKER), brevbestilling);

        assertThat(pdfResponse).isNotNull().isEqualTo(expectedPdf);

        verify(mockDokgenConsumer).lagPdf(any(), any(), eq(false), eq(false));
        verify(mockPersondataFasade, times(2)).hentPerson(any());
    }

    @Test
    void produserUtkastUtenRepresentantForBrukerOk() {
        when(mockDokgenConsumer.lagPdf(anyString(), any(), eq(false), eq(true))).thenReturn(expectedPdf);
        when(mockJoarkFasade.hentJournalpost(any())).thenReturn(lagJournalpost());
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling());
        when(mockPersondataFasade.hentPerson(anyString())).thenReturn(lagPersonopplysninger());
        when(mockKodeverkService.dekod(FellesKodeverk.POSTNUMMER, "0123")).thenReturn("Aker");
        when(mockKodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, "NO")).thenReturn("Norge");
        when(mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(true), eq(false))).thenReturn(
            List.of(lagBruker()));

        BrevbestillingRequest brevbestillingRequest = new BrevbestillingRequest.Builder()
            .medProduserbardokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medMottaker(Aktoersroller.BRUKER)
            .medBestillersId("Z123456")
            .build();


        byte[] pdfResponse = dokgenService.produserUtkast(123L, brevbestillingRequest);


        assertThat(pdfResponse).isNotNull().isEqualTo(expectedPdf);

        verify(mockDokgenConsumer).lagPdf(any(), any(), eq(false), eq(true));

        verify(mockSaksbehandlerService).hentNavnForIdent(anyString());
        verifyNoInteractions(mockEregFasade);
        verifyNoInteractions(mockKontaktOpplysningService);
    }

    @Test
    void produserUtkastTilRepresentantForBrukerOk() {
        when(mockDokgenConsumer.lagPdf(anyString(), any(), eq(false), eq(true))).thenReturn(expectedPdf);
        when(mockJoarkFasade.hentJournalpost(any())).thenReturn(lagJournalpost());
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling());
        when(mockPersondataFasade.hentPerson(anyString())).thenReturn(lagPersonopplysninger());
        when(mockEregFasade.hentOrganisasjon(any())).thenReturn(lagSaksopplysning());
        when(mockKontaktOpplysningService.hentKontaktopplysning(any(), any())).thenReturn(of(lagKontaktOpplysning()));

        when(mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(true), eq(false)))
            .thenReturn(List.of(lagRepresentant(ORGNR, Representerer.BRUKER)));

        BrevbestillingRequest brevbestillingRequest = new BrevbestillingRequest.Builder()
            .medProduserbardokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medMottaker(Aktoersroller.BRUKER)
            .medBestillersId("Z123456")
            .build();


        byte[] pdfResponse = dokgenService.produserUtkast(123L, brevbestillingRequest);


        assertThat(pdfResponse).isNotNull().isEqualTo(expectedPdf);

        verify(mockDokgenConsumer).lagPdf(any(), any(), eq(false), eq(true));
        verify(mockEregFasade).hentOrganisasjon(ORGNR);
        verify(mockKontaktOpplysningService).hentKontaktopplysning(any(), any());
    }

    @Test
    void produserUtkastTilRepresentantForArbeidsgiverOk() {
        when(mockDokgenConsumer.lagPdf(anyString(), any(), eq(false), eq(true))).thenReturn(expectedPdf);
        when(mockJoarkFasade.hentJournalpost(any())).thenReturn(lagJournalpost());
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling());
        when(mockPersondataFasade.hentPerson(anyString())).thenReturn(lagPersonopplysninger());
        when(mockEregFasade.hentOrganisasjon(any())).thenReturn(lagSaksopplysning());
        when(mockKontaktOpplysningService.hentKontaktopplysning(any(), any())).thenReturn(of(lagKontaktOpplysning()));

        when(mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(true), eq(false)))
            .thenReturn(List.of(lagRepresentant(ORGNR, Representerer.ARBEIDSGIVER)));

        BrevbestillingRequest brevbestillingRequest = new BrevbestillingRequest.Builder()
            .medProduserbardokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medMottaker(Aktoersroller.ARBEIDSGIVER)
            .medBestillersId("Z123456")
            .build();


        byte[] pdfResponse = dokgenService.produserUtkast(123L, brevbestillingRequest);


        assertThat(pdfResponse).isNotNull().isEqualTo(expectedPdf);

        verify(mockDokgenConsumer).lagPdf(any(), any(), eq(false), eq(true));
        verify(mockEregFasade).hentOrganisasjon(ORGNR);
        verify(mockKontaktOpplysningService).hentKontaktopplysning(any(), any());
    }

    @Test
    void skalProdusereOgDistribuereBrevTilBruker() {
        Aktoer bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);

        when(mockSaksbehandlerService.hentNavnForIdent(anyString())).thenReturn("Saksbehandler, Ole");
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(new Behandling());
        when(mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(false), eq(false))).thenReturn(List.of(bruker));
        BrevbestillingRequest brevbestillingRequest = new BrevbestillingRequest.Builder()
            .medProduserbardokument(MANGELBREV_BRUKER)
            .medMottaker(Aktoersroller.BRUKER)
            .medBestillersId("Z123456")
            .build();


        dokgenService.produserOgDistribuerBrev(123L, brevbestillingRequest);


        verify(mockProsessinstansService).opprettProsessinstansOpprettOgDistribuerBrev(any(Behandling.class), any(Aktoer.class), brevbestillingCaptor.capture());
        verify(mockBrevMottakerService).avklarMottakere(any(), any(), any(), eq(false), eq(false));
        verify(mockSaksbehandlerService).hentNavnForIdent(anyString());

        MangelbrevBrevbestilling brevbestilling = (MangelbrevBrevbestilling) brevbestillingCaptor.getValue();
        assertThat(brevbestilling).isNotNull();
        assertThat(brevbestilling).extracting(
            DokgenBrevbestilling::getProduserbartdokument,
            DokgenBrevbestilling::getBehandlingId,
            DokgenBrevbestilling::getSaksbehandlerNavn
        ).containsExactly(MANGELBREV_BRUKER, 123L, "Saksbehandler, Ole");
    }

    @Test
    void skalProdusereOgDistribuereBrevTilOrgnrUtenKopi() {
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(new Behandling());

        BrevbestillingRequest brevbestillingRequest = new BrevbestillingRequest.Builder()
            .medProduserbardokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medMottaker(Aktoersroller.ARBEIDSGIVER)
            .medOrgNr(ORGNR)
            .medBestillersId("Z123456")
            .build();


        dokgenService.produserOgDistribuerBrev(123L, brevbestillingRequest);


        verify(mockProsessinstansService).opprettProsessinstansOpprettOgDistribuerBrev(any(Behandling.class), any(Aktoer.class), brevbestillingCaptor.capture());
        verifyNoInteractions(mockBrevMottakerService);

        DokgenBrevbestilling brevbestilling = brevbestillingCaptor.getValue();
        assertThat(brevbestilling).isNotNull();
        assertThat(brevbestilling).extracting(
            DokgenBrevbestilling::getProduserbartdokument,
            DokgenBrevbestilling::getBehandlingId
        ).containsExactly(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, 123L);
    }

    @Test
    void skalProdusereOgDistribuereBrevTilOrgnrMedKopi() {
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(new Behandling());

        BrevbestillingRequest brevbestillingRequest = new BrevbestillingRequest.Builder()
            .medProduserbardokument(MANGELBREV_BRUKER)
            .medBestillersId("Z123456")
            .medManglerFritekst("Mangler")
            .medMottaker(Aktoersroller.ARBEIDSGIVER)
            .medOrgNr(ORGNR)
            .medKopiMottakere(List.of(new KopiMottaker(Aktoersroller.BRUKER, null, "1223", null)))
            .build();


        dokgenService.produserOgDistribuerBrev(123L, brevbestillingRequest);


        verify(mockProsessinstansService, times(2)).opprettProsessinstansOpprettOgDistribuerBrev(any(Behandling.class),
            any(Aktoer.class), brevbestillingCaptor.capture());
        verifyNoInteractions(mockBrevMottakerService);

        MangelbrevBrevbestilling brevbestilling = (MangelbrevBrevbestilling) brevbestillingCaptor.getValue();
        assertThat(brevbestilling).isNotNull();
        assertThat(brevbestilling).extracting(
            MangelbrevBrevbestilling::getProduserbartdokument,
            MangelbrevBrevbestilling::getBehandlingId,
            MangelbrevBrevbestilling::getManglerInfoFritekst
        ).containsExactly(MANGELBREV_BRUKER, 123L, "Mangler");
    }

    @Test
    void produserOgDistribuerBrev_skalDistribuereBrevMedVedlegg_nårBrevbestillingInneholderVedlegg() {
        Aktoer bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);

        when(mockSaksbehandlerService.hentNavnForIdent(anyString())).thenReturn("Saksbehandler, Ole");
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(new Behandling());
        when(mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(false), eq(false))).thenReturn(List.of(bruker));
        var saksvedleggDto = Arrays.asList(new SaksvedleggDto("100", "200"),
            new SaksvedleggDto("300", "400"));
        var fritekstvedleggDto = Arrays.asList(
            new FritekstvedleggDto("tittel1", "fritekst1"),
            new FritekstvedleggDto("tittel2", "fritekst2"));
        BrevbestillingRequest brevbestillingRequest = new BrevbestillingRequest.Builder()
            .medProduserbardokument(GENERELT_FRITEKSTBREV_BRUKER)
            .medBestillersId("Z123456")
            .medSaksvedlegg(saksvedleggDto)
            .medFritekstvedlegg(fritekstvedleggDto)
            .build();


        dokgenService.produserOgDistribuerBrev(123L, brevbestillingRequest);


        verify(mockProsessinstansService).opprettProsessinstansOpprettOgDistribuerBrev(any(Behandling.class), any(Aktoer.class), brevbestillingCaptor.capture());
        verify(mockBrevMottakerService).avklarMottakere(any(), any(), any(), eq(false), eq(false));
        verify(mockSaksbehandlerService).hentNavnForIdent(anyString());

        FritekstbrevBrevbestilling brevbestilling = (FritekstbrevBrevbestilling) brevbestillingCaptor.getValue();
        assertThat(brevbestilling.getSaksvedleggBestilling())
            .hasSize(2)
            .extracting(SaksvedleggBestilling::journalpostID, SaksvedleggBestilling::dokumentID)
            .containsExactlyInAnyOrder(Tuple.tuple("100", "200"), Tuple.tuple("300", "400"));
        assertThat(brevbestilling.getFritekstvedleggBestilling())
            .hasSize(2)
            .extracting(FritekstvedleggBestilling::tittel, FritekstvedleggBestilling::fritekst)
            .containsExactlyInAnyOrder(Tuple.tuple("tittel1", "fritekst1"), Tuple.tuple("tittel2", "fritekst2"));
    }

    @Test
    void produserOgDistribuerBrev_skalDistribuereBrevMedDistribusjonstype_når_fritekstbrev() {
        Aktoer bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);

        when(mockSaksbehandlerService.hentNavnForIdent(anyString())).thenReturn("Saksbehandler, Ole");
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(new Behandling());
        when(mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(false), eq(false))).thenReturn(List.of(bruker));
        var saksvedleggDto = Arrays.asList(new SaksvedleggDto("100", "200"),
            new SaksvedleggDto("300", "400"));
        var fritekstvedleggDto = Arrays.asList(
            new FritekstvedleggDto("tittel1", "fritekst1"),
            new FritekstvedleggDto("tittel2", "fritekst2"));
        BrevbestillingRequest brevbestillingRequest = new BrevbestillingRequest.Builder()
            .medProduserbardokument(GENERELT_FRITEKSTBREV_BRUKER)
            .medBestillersId("Z123456")
            .medDistribusjonsType(Distribusjonstype.ANNET)
            .medSaksvedlegg(saksvedleggDto)
            .medFritekstvedlegg(fritekstvedleggDto)
            .build();

        dokgenService.produserOgDistribuerBrev(123L, brevbestillingRequest);


        verify(mockProsessinstansService).opprettProsessinstansOpprettOgDistribuerBrev(any(Behandling.class), any(Aktoer.class), brevbestillingCaptor.capture());
        verify(mockBrevMottakerService).avklarMottakere(any(), any(), any(), eq(false), eq(false));
        verify(mockSaksbehandlerService).hentNavnForIdent(anyString());

        FritekstbrevBrevbestilling brevbestilling = (FritekstbrevBrevbestilling) brevbestillingCaptor.getValue();
        assertThat(brevbestilling.getSaksvedleggBestilling())
            .hasSize(2)
            .extracting(SaksvedleggBestilling::journalpostID, SaksvedleggBestilling::dokumentID)
            .containsExactlyInAnyOrder(Tuple.tuple("100", "200"), Tuple.tuple("300", "400"));
        assertThat(brevbestilling.getFritekstvedleggBestilling())
            .hasSize(2)
            .extracting(FritekstvedleggBestilling::tittel, FritekstvedleggBestilling::fritekst)
            .containsExactlyInAnyOrder(Tuple.tuple("tittel1", "fritekst1"), Tuple.tuple("tittel2", "fritekst2"));
        assertThat(brevbestilling.getDistribusjonstype()).isEqualTo(Distribusjonstype.ANNET);
    }

    @Test
    void produserOgDistribuerBrev_brukerSkalHaKopi_setterFeltKorrekt() {
        Aktoer arbeidsgiver = new Aktoer();
        arbeidsgiver.setRolle(Aktoersroller.ARBEIDSGIVER);
        when(mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(false), eq(false))).thenReturn(List.of(arbeidsgiver));
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(new Behandling());
        BrevbestillingRequest brevbestillingRequest = new BrevbestillingRequest.Builder()
            .medProduserbardokument(GENERELT_FRITEKSTBREV_BRUKER)
            .medKopiMottakere(List.of(new KopiMottaker(Aktoersroller.BRUKER, null, null, null)))
            .build();


        dokgenService.produserOgDistribuerBrev(123L, brevbestillingRequest);


        verify(mockProsessinstansService, times(2)).opprettProsessinstansOpprettOgDistribuerBrev(any(Behandling.class), any(Aktoer.class), brevbestillingCaptor.capture());
        FritekstbrevBrevbestilling brevbestilling = (FritekstbrevBrevbestilling) brevbestillingCaptor.getValue();
        assertThat(brevbestilling.isBrukerSkalHaKopi()).isTrue();
    }

    @Test
    void produserOgDistribuerBrev_brukerSkalIkkeHaKopi_setterFeltKorrekt() {
        Aktoer arbeidsgiver = new Aktoer();
        arbeidsgiver.setRolle(Aktoersroller.ARBEIDSGIVER);
        when(mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(false), eq(false))).thenReturn(List.of(arbeidsgiver));
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(new Behandling());
        BrevbestillingRequest brevbestillingRequest = new BrevbestillingRequest.Builder()
            .medProduserbardokument(MANGELBREV_ARBEIDSGIVER)
            .build();


        dokgenService.produserOgDistribuerBrev(123L, brevbestillingRequest);


        verify(mockProsessinstansService).opprettProsessinstansOpprettOgDistribuerBrev(any(Behandling.class), any(Aktoer.class), brevbestillingCaptor.capture());
        MangelbrevBrevbestilling brevbestilling = (MangelbrevBrevbestilling) brevbestillingCaptor.getValue();
        assertThat(brevbestilling.isBrukerSkalHaKopi()).isFalse();
    }

    @Test
    void erTilgjengeligDokgenmal() {
        unleash.enableAll();

        assertThat(dokgenService.erTilgjengeligDokgenmal(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)).isTrue();
        assertThat(dokgenService.erTilgjengeligDokgenmal(ATTEST_A1)).isFalse();
    }

    @Test
    void skalHenteDokumentInfo() {
        DokumentproduksjonsInfo dokumentproduksjonsInfo = dokgenService.hentDokumentInfo(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD);

        assertThat(dokumentproduksjonsInfo.dokgenMalnavn()).isEqualTo("saksbehandlingstid_soknad");
        assertThat(dokumentproduksjonsInfo.dokumentKategoriKode()).isEqualTo("IB");
        assertThat(dokumentproduksjonsInfo.journalføringsTittel()).isEqualTo("Melding om forventet saksbehandlingstid");
    }

    private Journalpost lagJournalpost() {
        Journalpost journalpost = new Journalpost("1234");
        journalpost.setForsendelseMottatt(Instant.now());
        journalpost.setAvsenderNavn("Mr. Avsender");
        journalpost.setAvsenderId(FNR);
        return journalpost;
    }

    private Saksopplysning lagSaksopplysning() {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(lagOrg());
        return saksopplysning;
    }

    private Aktoer lagBruker() {
        var aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.BRUKER);
        aktoer.setAktørId(FNR);
        return aktoer;
    }

    private Aktoer lagRepresentant(String mottakerID, Representerer representerer) {
        var aktoer = new Aktoer();
        aktoer.setRolle(Aktoersroller.REPRESENTANT);
        aktoer.setRepresenterer(representerer);
        if (mottakerID.equals(ORGNR)) {
            aktoer.setOrgnr(ORGNR);
        } else if (mottakerID.equals(FNR)) {
            aktoer.setPersonIdent(FNR);
        }
        return aktoer;
    }
}
