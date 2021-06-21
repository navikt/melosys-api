package no.nav.melosys.service.dokument;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.brev.DokgenBrevbestilling;
import no.nav.melosys.domain.brev.MangelbrevBrevbestilling;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.person.Informasjonsbehov;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.dokgen.DokgenConsumer;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.brev.BrevbestillingDto;
import no.nav.melosys.service.dokument.brev.KopiMottaker;
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DokgenServiceTest {

    public static final String FNR = "99887766554";

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

    @Captor
    private ArgumentCaptor<DokgenBrevbestilling> brevbestillingCaptor;

    private final FakeUnleash unleash = new FakeUnleash();

    private DokgenService dokgenService;

    private final byte[] expectedPdf = "pdf".getBytes();

    @BeforeEach
    void init() {
        dokgenService = new DokgenService(mockDokgenConsumer, new DokumentproduksjonsInfoMapper(unleash), mockJoarkFasade,
            new DokgenMalMapper(mockKodeverkService, mockBehandlingsresultatService, mockEregFasade, mockPersondataFasade),
            mockBehandlingsService,
            mockEregFasade, mockKontaktOpplysningService, mockBrevMottakerService, mockProsessinstansService);
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
    void produserBrevTilBrukerOk() throws Exception {
        when(mockDokgenConsumer.lagPdf(anyString(), any(), anyBoolean())).thenReturn(expectedPdf);
        when(mockJoarkFasade.hentJournalpost(any())).thenReturn(lagJournalpost());
        when(mockBehandlingsService.hentBehandling(anyLong())).thenReturn(lagBehandling());
        when(mockPersondataFasade.hentPersonFraTps(any(), any())).thenReturn(lagPersonopplysning());

        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.BRUKER);

        MangelbrevBrevbestilling brevbestilling = new MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(MANGELBREV_BRUKER)
            .medBehandlingId(123)
            .build();

        byte[] pdfResponse = dokgenService.produserBrev(mottaker, brevbestilling);

        assertThat(pdfResponse).isNotNull();
        assertThat(pdfResponse).isEqualTo(expectedPdf);

        verify(mockDokgenConsumer).lagPdf(any(), any(), eq(false));
        verifyNoInteractions(mockEregFasade);
        verifyNoInteractions(mockKontaktOpplysningService);
    }

    @Test
    void produserBrevTilRepresentantOk() {
        when(mockDokgenConsumer.lagPdf(anyString(), any(), anyBoolean())).thenReturn(expectedPdf);
        when(mockJoarkFasade.hentJournalpost(any())).thenReturn(lagJournalpost());
        when(mockBehandlingsService.hentBehandling(anyLong())).thenReturn(lagBehandling());
        when(mockEregFasade.hentOrganisasjon(any())).thenReturn(lagSaksopplysning());
        when(mockKontaktOpplysningService.hentKontaktopplysning(any(), any())).thenReturn(of(lagKontaktOpplysning()));

        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.REPRESENTANT);
        mottaker.setOrgnr("123456789");

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medBehandlingId(123)
            .build();

        byte[] pdfResponse = dokgenService.produserBrev(mottaker, brevbestilling);

        assertThat(pdfResponse).isNotNull();
        assertThat(pdfResponse).isEqualTo(expectedPdf);

        verify(mockDokgenConsumer).lagPdf(any(), any(), eq(false));
        verify(mockEregFasade).hentOrganisasjon(any());
        verify(mockKontaktOpplysningService).hentKontaktopplysning(any(), any());
    }

    @Test
    void produserUtkastUtenRepresentantForBrukerOk() {
        when(mockDokgenConsumer.lagPdf(anyString(), any(), anyBoolean())).thenReturn(expectedPdf);
        when(mockJoarkFasade.hentJournalpost(any())).thenReturn(lagJournalpost());
        when(mockBehandlingsService.hentBehandling(anyLong())).thenReturn(lagBehandling());
        when(mockPersondataFasade.hentPersonFraTps(any(), any())).thenReturn(lagPersonopplysning());
        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.BRUKER);
        when(mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(true), eq(false))).thenReturn(asList(mottaker));

        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder()
            .medProduserbardokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medMottaker(Aktoersroller.BRUKER)
            .build();

        byte[] pdfResponse = dokgenService.produserUtkast(123L, brevbestillingDto);

        assertThat(pdfResponse).isNotNull();
        assertThat(pdfResponse).isEqualTo(expectedPdf);

        verify(mockDokgenConsumer).lagPdf(any(), any(), eq(true));
        verify(mockPersondataFasade).hentPersonFraTps(any(), eq(Informasjonsbehov.STANDARD));

        verifyNoInteractions(mockEregFasade);
        verifyNoInteractions(mockKontaktOpplysningService);
    }

    @Test
    void produserUtkastTilRepresentantForBrukerOk() {
        when(mockDokgenConsumer.lagPdf(anyString(), any(), anyBoolean())).thenReturn(expectedPdf);
        when(mockJoarkFasade.hentJournalpost(any())).thenReturn(lagJournalpost());
        when(mockBehandlingsService.hentBehandling(anyLong())).thenReturn(lagBehandling());
        when(mockEregFasade.hentOrganisasjon(any())).thenReturn(lagSaksopplysning());
        when(mockKontaktOpplysningService.hentKontaktopplysning(any(), any())).thenReturn(of(lagKontaktOpplysning()));

        Aktoer representant = new Aktoer();
        representant.setRolle(Aktoersroller.REPRESENTANT);
        representant.setOrgnr("987654321");
        when(mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(true), eq(false))).thenReturn(asList(representant));

        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.BRUKER);

        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder()
            .medProduserbardokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medMottaker(Aktoersroller.BRUKER)
            .build();

        byte[] pdfResponse = dokgenService.produserUtkast(123L, brevbestillingDto);

        assertThat(pdfResponse).isNotNull();
        assertThat(pdfResponse).isEqualTo(expectedPdf);

        verify(mockDokgenConsumer).lagPdf(any(), any(), eq(true));
        verify(mockEregFasade).hentOrganisasjon(eq("987654321"));
        verify(mockKontaktOpplysningService).hentKontaktopplysning(any(), any());
    }

    @Test
    void produserUtkastTilRepresentantForArbeidsgiverOk() throws Exception {
        when(mockDokgenConsumer.lagPdf(anyString(), any(), anyBoolean())).thenReturn(expectedPdf);
        when(mockJoarkFasade.hentJournalpost(any())).thenReturn(lagJournalpost());
        when(mockBehandlingsService.hentBehandling(anyLong())).thenReturn(lagBehandling());
        when(mockEregFasade.hentOrganisasjon(any())).thenReturn(lagSaksopplysning());
        when(mockKontaktOpplysningService.hentKontaktopplysning(any(), any())).thenReturn(of(lagKontaktOpplysning()));

        Aktoer representant = new Aktoer();
        representant.setRolle(Aktoersroller.REPRESENTANT);
        representant.setOrgnr("987654321");
        when(mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(true), eq(false))).thenReturn(asList(representant));

        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.ARBEIDSGIVER);
        mottaker.setOrgnr("123456789");

        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder()
            .medProduserbardokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medMottaker(Aktoersroller.BRUKER)
            .build();

        byte[] pdfResponse = dokgenService.produserUtkast(123L, brevbestillingDto);

        assertThat(pdfResponse).isNotNull();
        assertThat(pdfResponse).isEqualTo(expectedPdf);

        verify(mockDokgenConsumer).lagPdf(any(), any(), eq(true));
        verify(mockEregFasade).hentOrganisasjon(eq("987654321"));
        verify(mockKontaktOpplysningService).hentKontaktopplysning(any(), any());
    }

    @Test
    void skalProdusereOgDistribuereBrevTilBruker() throws Exception {
        Aktoer bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);

        when(mockBehandlingsService.hentBehandling(anyLong())).thenReturn(new Behandling());
        when(mockBrevMottakerService.avklarMottakere(any(), any(), any(), eq(false), eq(false))).thenReturn(List.of(bruker));
        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder()
            .medProduserbardokument(MANGELBREV_BRUKER)
            .medMottaker(Aktoersroller.BRUKER)
            .build();

        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto);

        verify(mockProsessinstansService).opprettProsessinstansOpprettOgDistribuerBrev(any(Behandling.class), any(Aktoer.class), brevbestillingCaptor.capture());
        verify(mockBrevMottakerService).avklarMottakere(any(), any(), any(), eq(false), eq(false));

        MangelbrevBrevbestilling brevbestilling = (MangelbrevBrevbestilling) brevbestillingCaptor.getValue();
        assertThat(brevbestilling).isNotNull();
        assertThat(brevbestilling).extracting(
            DokgenBrevbestilling::getProduserbartdokument,
            DokgenBrevbestilling::getBehandlingId
        ).containsExactly(MANGELBREV_BRUKER, 123L);
    }

    @Test
    void skalProdusereOgDistribuereBrevTilOrgnrUtenKopi() throws Exception {
        when(mockBehandlingsService.hentBehandling(anyLong())).thenReturn(new Behandling());

        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder()
            .medProduserbardokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .medMottaker(Aktoersroller.ARBEIDSGIVER)
            .medOrgNr("987654321")
            .build();

        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto);

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
    void skalProdusereOgDistribuereBrevTilOrgnrMedKopi() throws Exception {
        when(mockBehandlingsService.hentBehandling(anyLong())).thenReturn(new Behandling());

        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder()
            .medProduserbardokument(MANGELBREV_BRUKER)
            .medManglerFritekst("Mangler")
            .medMottaker(Aktoersroller.ARBEIDSGIVER)
            .medOrgNr("987654321")
            .medKopiMottakere(List.of(new KopiMottaker(Aktoersroller.BRUKER, null, "1223")))
            .build();

        dokgenService.produserOgDistribuerBrev(123L, brevbestillingDto);

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
    void erTilgjengeligDokgenmal() {
        unleash.enableAll();

        assertThat(dokgenService.erTilgjengeligDokgenmal(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)).isTrue();
        assertThat(dokgenService.erTilgjengeligDokgenmal(ATTEST_A1)).isFalse();
    }

    @Test
    void skalHenteDokumentInfo() throws Exception {
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

    private Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(1L);
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setSaksopplysninger(singleton(lagPersonopplysning()));
        behandling.setFagsak(lagFagsak(behandling));
        return behandling;
    }

    private Saksopplysning lagPersonopplysning() {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PERSOPL);
        PersonDokument personDokument = new PersonDokument();
        personDokument.setFnr("99887766554");
        saksopplysning.setDokument(personDokument);
        return saksopplysning;
    }

    private Fagsak lagFagsak(Behandling behandling) {
        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(123L);
        fagsak.setType(Sakstyper.EU_EOS);
        fagsak.setBehandlinger(List.of(behandling));
        return fagsak;
    }

    private Saksopplysning lagSaksopplysning() {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setDokument(lagOrg());
        return saksopplysning;
    }

    private OrganisasjonDokument lagOrg() {
        OrganisasjonDokument organisasjonDokument = new OrganisasjonDokument();
        organisasjonDokument.setOrgnummer("122344");
        organisasjonDokument.setOrganisasjonDetaljer(lagOrgDetaljer());
        return organisasjonDokument;
    }

    private OrganisasjonsDetaljer lagOrgDetaljer() {
        OrganisasjonsDetaljer organisasjonsDetaljer = new OrganisasjonsDetaljer();
        organisasjonsDetaljer.postadresse = singletonList(lagOrgAdresse());
        return organisasjonsDetaljer;
    }

    private GeografiskAdresse lagOrgAdresse() {
        SemistrukturertAdresse semistrukturertAdresse = new SemistrukturertAdresse();
        semistrukturertAdresse.setGyldighetsperiode(new Periode(LocalDate.now().minusDays(2), LocalDate.now().plusDays(2)));
        return semistrukturertAdresse;
    }

    private Kontaktopplysning lagKontaktOpplysning() {
        Kontaktopplysning kontaktopplysning = new Kontaktopplysning();
        kontaktopplysning.setKontaktNavn("Donald Duck");
        return kontaktopplysning;
    }

}
