package no.nav.melosys.service.dokument.brev.mapper;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.FellesKodeverk;
import no.nav.melosys.domain.Saksopplysning;
import no.nav.melosys.domain.arkiv.Distribusjonstype;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.arkiv.SaksvedleggBestilling;
import no.nav.melosys.domain.brev.*;
import no.nav.melosys.domain.kodeverk.Land_iso2;
import no.nav.melosys.domain.kodeverk.Mottakerroller;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.DokgenConsumer;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.behandling.UtledMottaksdato;
import no.nav.melosys.service.bruker.SaksbehandlerService;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.DokumentHentingService;
import no.nav.melosys.service.dokument.DokumentproduksjonsInfo;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.FritekstvedleggDto;
import no.nav.melosys.service.dokument.brev.KopiMottakerDto;
import no.nav.melosys.service.dokument.brev.SaksvedleggDto;
import no.nav.melosys.service.kodeverk.KodeverkService;
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
        DokgenMapperDatahenter dokgenMapperDatahenter = new DokgenMapperDatahenter(mockBehandlingsresultatService, mockEregFasade,
            mockPersondataFasade, mockDokumentHentingService, mockKodeverkService);

        dokgenService = new DokgenService(mockDokgenConsumer, new DokumentproduksjonsInfoMapper(unleash),
            mockJoarkFasade,
            new DokgenMalMapper(dokgenMapperDatahenter, mockInnvilgelseFtrlMapper, mockTrygdeavtaleMapper),
            mockBehandlingsService, mockEregFasade, mockKontaktOpplysningService,
            mockBrevMottakerService, mockProsessinstansService, mockSaksbehandlerService,
            mockUtenlandskMyndighetService, mockUtledMottaksdato);

        reset(mockDokgenConsumer);
    }

    @Test
    void produserBrevFeilerUtilgjengeligMal() {
        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(ATTEST_A1)
            .build();

        assertThatThrownBy(() -> dokgenService.produserBrev(new Mottaker(), brevbestilling))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("ProduserbartDokument ATTEST_A1 er ikke støttet");
    }

    @Test
    void produserBrevTilBrukerOk() {
        when(mockDokgenConsumer.lagPdf(anyString(), any(), eq(false), eq(false))).thenReturn(expectedPdf);
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling());
        when(mockPersondataFasade.hentPerson(anyString())).thenReturn(lagPersonopplysninger());
        when(mockKodeverkService.dekod(FellesKodeverk.POSTNUMMER, "0123")).thenReturn("Aker");
        when(mockKodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, "NO")).thenReturn("Norge");

        Mottaker mottaker = lagBruker();

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
    void produserBrev_henterDatoFraUtledMottaksdato() {
        when(mockDokgenConsumer.lagPdf(anyString(), any(), eq(false), eq(false))).thenReturn(expectedPdf);
        var journalpost = lagJournalpost();
        when(mockJoarkFasade.hentJournalpost("journalpostId")).thenReturn(journalpost);
        var behandling = lagBehandling();
        behandling.setInitierendeJournalpostId("journalpostId");
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(behandling);
        when(mockPersondataFasade.hentPerson(anyString())).thenReturn(lagPersonopplysninger());
        when(mockKodeverkService.dekod(FellesKodeverk.POSTNUMMER, "0123")).thenReturn("Aker");
        when(mockKodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, "NO")).thenReturn("Norge");

        Mottaker mottaker = lagBruker();

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
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling());
        when(mockPersondataFasade.hentPerson(anyString())).thenReturn(lagPersonopplysninger());
        when(mockEregFasade.hentOrganisasjon(any())).thenReturn(lagSaksopplysning());
        when(mockKontaktOpplysningService.hentKontaktopplysning(any(), any())).thenReturn(of(lagKontaktOpplysning()));
        when(mockUtledMottaksdato.getMottaksdato(any(), any())).thenReturn(LocalDate.now());

        Mottaker mottaker = lagRepresentant(ORGNR);

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
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling());
        when(mockPersondataFasade.hentPerson(anyString())).thenReturn(lagPersonopplysninger());
        when(mockKodeverkService.dekod(FellesKodeverk.POSTNUMMER, "0123")).thenReturn("Aker");
        when(mockKodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, "NO")).thenReturn("Norge");
        when(mockUtledMottaksdato.getMottaksdato(any(), any())).thenReturn(LocalDate.now());

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medBehandlingId(123)
            .build();

        byte[] pdfResponse = dokgenService.produserBrev(lagRepresentant(FNR), brevbestilling);

        assertThat(pdfResponse).isNotNull().isEqualTo(expectedPdf);

        verify(mockDokgenConsumer).lagPdf(any(), any(), eq(false), eq(false));
        verify(mockPersondataFasade, times(2)).hentPerson(any());
    }

    @Test
    void produserUtkastUtenRepresentantForBrukerOk() {
        when(mockDokgenConsumer.lagPdf(anyString(), any(), eq(false), eq(true))).thenReturn(expectedPdf);
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling());
        when(mockPersondataFasade.hentPerson(anyString())).thenReturn(lagPersonopplysninger());
        when(mockKodeverkService.dekod(FellesKodeverk.POSTNUMMER, "0123")).thenReturn("Aker");
        when(mockKodeverkService.dekod(FellesKodeverk.LANDKODER_ISO2, "NO")).thenReturn("Norge");
        when(mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(true), eq(false))).thenReturn(
            List.of(lagBruker()));
        when(mockUtledMottaksdato.getMottaksdato(any(), any())).thenReturn(LocalDate.now());

        BrevbestillingDto brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setProduserbardokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD);
        brevbestillingDto.setMottaker(Mottakerroller.BRUKER);
        brevbestillingDto.setBestillersId("Z123456");

        byte[] pdfResponse = dokgenService.produserUtkast(123L, brevbestillingDto);


        assertThat(pdfResponse).isNotNull().isEqualTo(expectedPdf);

        verify(mockDokgenConsumer).lagPdf(any(), any(), eq(false), eq(true));

        verify(mockSaksbehandlerService).hentNavnForIdent(anyString());
        verifyNoInteractions(mockEregFasade);
        verifyNoInteractions(mockKontaktOpplysningService);
    }

    @Test
    void produserUtkastTilRepresentantForBrukerOk() {
        when(mockDokgenConsumer.lagPdf(anyString(), any(), eq(false), eq(true))).thenReturn(expectedPdf);
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling());
        when(mockPersondataFasade.hentPerson(anyString())).thenReturn(lagPersonopplysninger());
        when(mockEregFasade.hentOrganisasjon(any())).thenReturn(lagSaksopplysning());
        when(mockKontaktOpplysningService.hentKontaktopplysning(any(), any())).thenReturn(of(lagKontaktOpplysning()));
        when(mockUtledMottaksdato.getMottaksdato(any(), any())).thenReturn(LocalDate.now());

        when(mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(true), eq(false)))
            .thenReturn(List.of(lagRepresentant(ORGNR)));

        var brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setProduserbardokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD);
        brevbestillingDto.setMottaker(Mottakerroller.BRUKER);
        brevbestillingDto.setBestillersId("Z123456");


        byte[] pdfResponse = dokgenService.produserUtkast(123L, brevbestillingDto);


        assertThat(pdfResponse).isNotNull().isEqualTo(expectedPdf);

        verify(mockDokgenConsumer).lagPdf(any(), any(), eq(false), eq(true));
        verify(mockEregFasade).hentOrganisasjon(ORGNR);
        verify(mockKontaktOpplysningService).hentKontaktopplysning(any(), any());
    }

    @Test
    void produserUtkastTilRepresentantForArbeidsgiverOk() {
        when(mockDokgenConsumer.lagPdf(anyString(), any(), eq(false), eq(true))).thenReturn(expectedPdf);
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(lagBehandling());
        when(mockPersondataFasade.hentPerson(anyString())).thenReturn(lagPersonopplysninger());
        when(mockEregFasade.hentOrganisasjon(any())).thenReturn(lagSaksopplysning());
        when(mockKontaktOpplysningService.hentKontaktopplysning(any(), any())).thenReturn(of(lagKontaktOpplysning()));
        when(mockUtledMottaksdato.getMottaksdato(any(), any())).thenReturn(LocalDate.now());

        when(mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(true), eq(false)))
            .thenReturn(List.of(lagRepresentant(ORGNR)));

        var brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setProduserbardokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD);
        brevbestillingDto.setMottaker(Mottakerroller.ARBEIDSGIVER);
        brevbestillingDto.setBestillersId("Z123456");


        byte[] pdfResponse = dokgenService.produserUtkast(123L, brevbestillingDto);


        assertThat(pdfResponse).isNotNull().isEqualTo(expectedPdf);

        verify(mockDokgenConsumer).lagPdf(any(), any(), eq(false), eq(true));
        verify(mockEregFasade).hentOrganisasjon(ORGNR);
        verify(mockKontaktOpplysningService).hentKontaktopplysning(any(), any());
    }

    @Test
    void skalProdusereOgDistribuereBrevTilBruker() {
        Mottaker bruker = Mottaker.medRolle(Mottakerroller.BRUKER);

        when(mockSaksbehandlerService.hentNavnForIdent(anyString())).thenReturn("Saksbehandler, Ole");
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(new Behandling());
        when(mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(false), eq(false))).thenReturn(List.of(bruker));

        var brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setProduserbardokument(MANGELBREV_BRUKER);
        brevbestillingDto.setMottaker(Mottakerroller.BRUKER);
        brevbestillingDto.setBestillersId("Z123456");


        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto);


        verify(mockProsessinstansService).opprettProsessinstansOpprettOgDistribuerBrev(any(Behandling.class), any(Mottaker.class), brevbestillingCaptor.capture());
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

        var brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setProduserbardokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD);
        brevbestillingDto.setMottaker(Mottakerroller.ARBEIDSGIVER);
        brevbestillingDto.setOrgnr(ORGNR);
        brevbestillingDto.setBestillersId("Z123456");


        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto);


        verify(mockProsessinstansService).opprettProsessinstansOpprettOgDistribuerBrev(any(Behandling.class), any(Mottaker.class), brevbestillingCaptor.capture());
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

        var brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setProduserbardokument(MANGELBREV_BRUKER);
        brevbestillingDto.setMottaker(Mottakerroller.ARBEIDSGIVER);
        brevbestillingDto.setOrgnr(ORGNR);
        brevbestillingDto.setManglerFritekst("Mangler");
        brevbestillingDto.setBestillersId("Z123456");
        brevbestillingDto.setKopiMottakere(List.of(new KopiMottakerDto(Mottakerroller.BRUKER, null, "1223", null)));


        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto);


        verify(mockProsessinstansService, times(2)).opprettProsessinstansOpprettOgDistribuerBrev(any(Behandling.class),
            any(Mottaker.class), brevbestillingCaptor.capture());
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
        Mottaker bruker = Mottaker.medRolle(Mottakerroller.BRUKER);

        when(mockSaksbehandlerService.hentNavnForIdent(anyString())).thenReturn("Saksbehandler, Ole");
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(new Behandling());
        when(mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(false), eq(false))).thenReturn(List.of(bruker));
        var saksvedleggDto = Arrays.asList(new SaksvedleggDto("100", "200"),
            new SaksvedleggDto("300", "400"));
        var fritekstvedleggDto = Arrays.asList(
            new FritekstvedleggDto("tittel1", "fritekst1"),
            new FritekstvedleggDto("tittel2", "fritekst2"));

        var brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setProduserbardokument(GENERELT_FRITEKSTBREV_BRUKER);
        brevbestillingDto.setSaksVedlegg(saksvedleggDto);
        brevbestillingDto.setFritekstvedlegg(fritekstvedleggDto);
        brevbestillingDto.setBestillersId("Z123456");


        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto);


        verify(mockProsessinstansService).opprettProsessinstansOpprettOgDistribuerBrev(any(Behandling.class), any(Mottaker.class), brevbestillingCaptor.capture());
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
        Mottaker bruker = Mottaker.medRolle(Mottakerroller.BRUKER);

        when(mockSaksbehandlerService.hentNavnForIdent(anyString())).thenReturn("Saksbehandler, Ole");
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(new Behandling());
        when(mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(false), eq(false))).thenReturn(List.of(bruker));
        var saksvedleggDto = Arrays.asList(new SaksvedleggDto("100", "200"),
            new SaksvedleggDto("300", "400"));
        var fritekstvedleggDto = Arrays.asList(
            new FritekstvedleggDto("tittel1", "fritekst1"),
            new FritekstvedleggDto("tittel2", "fritekst2"));

        var brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setProduserbardokument(GENERELT_FRITEKSTBREV_BRUKER);
        brevbestillingDto.setDistribusjonstype(Distribusjonstype.ANNET);
        brevbestillingDto.setSaksVedlegg(saksvedleggDto);
        brevbestillingDto.setFritekstvedlegg(fritekstvedleggDto);
        brevbestillingDto.setBestillersId("Z123456");


        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto);


        verify(mockProsessinstansService).opprettProsessinstansOpprettOgDistribuerBrev(any(Behandling.class), any(Mottaker.class), brevbestillingCaptor.capture());
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
        Mottaker arbeidsgiver = Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER);
        when(mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(false), eq(false))).thenReturn(List.of(arbeidsgiver));
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(new Behandling());

        var brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setProduserbardokument(GENERELT_FRITEKSTBREV_BRUKER);
        brevbestillingDto.setKopiMottakere(List.of(new KopiMottakerDto(Mottakerroller.BRUKER, null, null, null)));


        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto);


        verify(mockProsessinstansService, times(2)).opprettProsessinstansOpprettOgDistribuerBrev(any(Behandling.class), any(Mottaker.class), brevbestillingCaptor.capture());
        FritekstbrevBrevbestilling brevbestilling = (FritekstbrevBrevbestilling) brevbestillingCaptor.getValue();
        assertThat(brevbestilling.isBrukerSkalHaKopi()).isTrue();
    }

    @Test
    void produserOgDistribuerBrev_brukerSkalIkkeHaKopi_setterFeltKorrekt() {
        Mottaker arbeidsgiver = Mottaker.medRolle(Mottakerroller.ARBEIDSGIVER);
        when(mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(false), eq(false))).thenReturn(List.of(arbeidsgiver));
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(new Behandling());

        var brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setProduserbardokument(MANGELBREV_ARBEIDSGIVER);
        brevbestillingDto.setDistribusjonstype(Distribusjonstype.ANNET);


        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto);


        verify(mockProsessinstansService).opprettProsessinstansOpprettOgDistribuerBrev(any(Behandling.class), any(Mottaker.class), brevbestillingCaptor.capture());
        MangelbrevBrevbestilling brevbestilling = (MangelbrevBrevbestilling) brevbestillingCaptor.getValue();
        assertThat(brevbestilling.isBrukerSkalHaKopi()).isFalse();
    }

    @Test
    void skalProdusereOgDistribuereBrevTilFullmektigPrivatpersonMedKopi() {
        when(mockBehandlingsService.hentBehandlingMedSaksopplysninger(anyLong())).thenReturn(new Behandling());

        var brevbestillingDto = new BrevbestillingDto();
        brevbestillingDto.setProduserbardokument(MANGELBREV_ARBEIDSGIVER);
        brevbestillingDto.setMottaker(Mottakerroller.ARBEIDSGIVER);
        brevbestillingDto.setOrgnr(ORGNR);
        brevbestillingDto.setManglerFritekst("Mangler");
        brevbestillingDto.setBestillersId("Z123456");
        brevbestillingDto.setKopiMottakere(List.of(new KopiMottakerDto(Mottakerroller.FULLMEKTIG, null, null, null)));

        var mottaker = new Mottaker(Mottakerroller.FULLMEKTIG, null, "12345678999", null, null, Land_iso2.NO);
        when(mockBrevMottakerService.avklarMottaker(any(), any(), any())).thenReturn(mottaker);

        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto);


        verify(mockProsessinstansService, times(1)).opprettProsessinstansOpprettOgDistribuerBrev(any(Behandling.class),
            eq(mottaker), brevbestillingCaptor.capture());

        var brevbestilling = (MangelbrevBrevbestilling) brevbestillingCaptor.getValue();
        assertThat(brevbestilling).extracting(
            MangelbrevBrevbestilling::getProduserbartdokument,
            MangelbrevBrevbestilling::getBehandlingId,
            MangelbrevBrevbestilling::getManglerInfoFritekst,
            MangelbrevBrevbestilling::isBestillKopi
        ).containsExactly(MANGELBREV_ARBEIDSGIVER, 123L, "Mangler", true);
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

    private Mottaker lagBruker() {
        var mottaker = Mottaker.medRolle(Mottakerroller.BRUKER);
        mottaker.setAktørId(FNR);
        return mottaker;
    }

    private Mottaker lagRepresentant(String mottakerID) {
        var mott = Mottaker.medRolle(Mottakerroller.FULLMEKTIG);
        if (mottakerID.equals(ORGNR)) {
            mott.setOrgnr(ORGNR);
        } else if (mottakerID.equals(FNR)) {
            mott.setPersonIdent(FNR);
        }
        return mott;
    }
}
