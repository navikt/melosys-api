package no.nav.melosys.service.dokument;

import java.time.Instant;
import java.time.LocalDate;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.arkiv.Journalpost;
import no.nav.melosys.domain.dokument.felles.Periode;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonDokument;
import no.nav.melosys.domain.dokument.organisasjon.OrganisasjonsDetaljer;
import no.nav.melosys.domain.dokument.organisasjon.adresse.GeografiskAdresse;
import no.nav.melosys.domain.dokument.organisasjon.adresse.SemistrukturertAdresse;
import no.nav.melosys.domain.dokument.person.PersonDokument;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
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
import no.nav.melosys.service.kodeverk.KodeverkService;
import no.nav.melosys.service.persondata.PersondataFasade;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
        assertThatThrownBy(() -> dokgenService.produserBrev(ATTEST_A1, 123L, null, null))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("ProduserbartDokument ATTEST_A1 er ikke støttet");
    }

    @Test
    void produserBrevTilBrukerOk() throws Exception {
        when(mockDokgenConsumer.lagPdf(anyString(), any(), anyBoolean())).thenReturn(expectedPdf);
        when(mockJoarkFasade.hentJournalpost(any())).thenReturn(lagJournalpost());
        when(mockBehandlingsService.hentBehandling(anyLong())).thenReturn(lagBehandling());
        when(mockPersondataFasade.hentPerson(any(), any())).thenReturn(lagPersonopplysning());

        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.BRUKER);

        byte[] pdfResponse = dokgenService.produserBrev(MANGELBREV_BRUKER, 123L, mottaker.getOrgnr(), new BrevbestillingDto.Builder().build());

        assertThat(pdfResponse).isNotNull();
        assertThat(pdfResponse).isEqualTo(expectedPdf);

        verify(mockDokgenConsumer).lagPdf(any(), any(), eq(false));
        verifyNoInteractions(mockEregFasade);
        verifyNoInteractions(mockKontaktOpplysningService);
    }

    @Test
    void produserBrevTilRepresentantOk() throws Exception {
        when(mockDokgenConsumer.lagPdf(anyString(), any(), anyBoolean())).thenReturn(expectedPdf);
        when(mockJoarkFasade.hentJournalpost(any())).thenReturn(lagJournalpost());
        when(mockBehandlingsService.hentBehandling(anyLong())).thenReturn(lagBehandling());
        when(mockEregFasade.hentOrganisasjon(any())).thenReturn(lagSaksopplysning());
        when(mockKontaktOpplysningService.hentKontaktopplysning(any(), any())).thenReturn(of(lagKontaktOpplysning()));

        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.REPRESENTANT);
        mottaker.setOrgnr("123456789");

        byte[] pdfResponse = dokgenService.produserBrev(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, 123L, mottaker.getOrgnr(), new BrevbestillingDto.Builder().build());

        assertThat(pdfResponse).isNotNull();
        assertThat(pdfResponse).isEqualTo(expectedPdf);

        verify(mockDokgenConsumer).lagPdf(any(), any(), eq(false));
        verify(mockEregFasade).hentOrganisasjon(any());
        verify(mockKontaktOpplysningService).hentKontaktopplysning(any(), any());
    }

    @Test
    void produserUtkastUtenRepresentantForBrukerOk() throws Exception {
        when(mockDokgenConsumer.lagPdf(anyString(), any(), anyBoolean())).thenReturn(expectedPdf);
        when(mockJoarkFasade.hentJournalpost(any())).thenReturn(lagJournalpost());
        when(mockBehandlingsService.hentBehandling(anyLong())).thenReturn(lagBehandling());
        when(mockPersondataFasade.hentPerson(any(), any())).thenReturn(lagPersonopplysning());
        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.BRUKER);
        when(mockBrevMottakerService.avklarMottakere(any(), any(), any())).thenReturn(asList(mottaker));

        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder()
            .medMottaker(Aktoersroller.BRUKER)
            .build();

        byte[] pdfResponse = dokgenService.produserUtkast(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, 123L, null, brevbestillingDto);

        assertThat(pdfResponse).isNotNull();
        assertThat(pdfResponse).isEqualTo(expectedPdf);

        verify(mockDokgenConsumer).lagPdf(any(), any(), eq(true));
        verify(mockPersondataFasade).hentPerson(any(), eq(Informasjonsbehov.STANDARD));

        verifyNoInteractions(mockEregFasade);
        verifyNoInteractions(mockKontaktOpplysningService);
    }

    @Test
    void produserUtkastTilRepresentantForBrukerOk() throws Exception {
        when(mockDokgenConsumer.lagPdf(anyString(), any(), anyBoolean())).thenReturn(expectedPdf);
        when(mockJoarkFasade.hentJournalpost(any())).thenReturn(lagJournalpost());
        when(mockBehandlingsService.hentBehandling(anyLong())).thenReturn(lagBehandling());
        when(mockEregFasade.hentOrganisasjon(any())).thenReturn(lagSaksopplysning());
        when(mockKontaktOpplysningService.hentKontaktopplysning(any(), any())).thenReturn(of(lagKontaktOpplysning()));

        Aktoer representant = new Aktoer();
        representant.setRolle(Aktoersroller.REPRESENTANT);
        representant.setOrgnr("987654321");
        when(mockBrevMottakerService.avklarMottakere(any(), any(), any())).thenReturn(asList(representant));

        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.BRUKER);

        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder()
            .medMottaker(Aktoersroller.BRUKER)
            .build();

        byte[] pdfResponse = dokgenService.produserUtkast(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, 123L, null, brevbestillingDto);

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
        when(mockBrevMottakerService.avklarMottakere(any(), any(), any())).thenReturn(asList(representant));

        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(Aktoersroller.ARBEIDSGIVER);
        mottaker.setOrgnr("123456789");

        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder()
            .medMottaker(Aktoersroller.BRUKER)
            .build();

        byte[] pdfResponse = dokgenService.produserUtkast(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD, 123L, mottaker.getOrgnr(), brevbestillingDto);

        assertThat(pdfResponse).isNotNull();
        assertThat(pdfResponse).isEqualTo(expectedPdf);

        verify(mockDokgenConsumer).lagPdf(any(), any(), eq(true));
        verify(mockEregFasade).hentOrganisasjon(eq("987654321"));
        verify(mockKontaktOpplysningService).hentKontaktopplysning(any(), any());
    }

    @Test
    void skalProdusereOgDistribuereBrev() throws Exception {
        Aktoer bruker = new Aktoer();
        bruker.setRolle(Aktoersroller.BRUKER);

        when(mockBehandlingsService.hentBehandling(anyLong())).thenReturn(new Behandling());
        when(mockBrevMottakerService.avklarMottakere(any(), any(), any())).thenReturn(asList(bruker));
        BrevbestillingDto brevbestillingDto = new BrevbestillingDto.Builder().build();

        dokgenService.produserOgDistribuerBrev(MANGELBREV_BRUKER, 123L, brevbestillingDto);

        verify(mockProsessinstansService).opprettProsessinstansOpprettOgDistribuerBrev(eq(MANGELBREV_BRUKER), any(), eq(bruker), any());
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
        personDokument.fnr = "99887766554";
        saksopplysning.setDokument(personDokument);
        return saksopplysning;
    }

    private Fagsak lagFagsak(Behandling behandling) {
        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(123L);
        fagsak.setBehandlinger(asList(behandling));
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