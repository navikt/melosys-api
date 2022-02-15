package no.nav.melosys.saksflyt.steg.brev;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Lovvalgsperiode;
import no.nav.melosys.domain.UtenlandskMyndighet;
import no.nav.melosys.domain.arkiv.OpprettJournalpost;
import no.nav.melosys.domain.brev.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.LovvalgBestemmelse;
import no.nav.melosys.domain.kodeverk.begrunnelser.Nyvurderingbakgrunner;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.saksflyt.TestdataFactory;
import no.nav.melosys.service.LovvalgsperiodeService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.dokument.DokgenService;
import no.nav.melosys.service.dokument.brev.mapper.DokumentproduksjonsInfoMapper;
import no.nav.melosys.service.persondata.PersondataFasade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.stream.Stream;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk.UK_ART6_1;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_trygdeavtale_uk.UK_ART8_2;
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
    private LovvalgsperiodeService mockLovvalgsperiodeService;

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

    @Captor
    ArgumentCaptor<OpprettJournalpost> opprettJournalpostCaptor;

    private OpprettJournalforBrev opprettJournalforBrev;

    @BeforeEach
    void init() {
        opprettJournalforBrev = new OpprettJournalforBrev(mockBehandlingService, mockLovvalgsperiodeService, mockDokgenService,
            mockUtenlandskMyndighetService, mockJoarkFasade, mockPersondataFasade, mockEregFasade, new FakeUnleash());
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
        assertThatThrownBy(() -> opprettJournalforBrev.utfør(new Prosessinstans()))
            .isInstanceOf(FunksjonellException.class)
            .hasMessage("Mangler mottaker");
    }

    @Test
    void utførOpprettJournalforBrevTilBruker() {
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(mockJoarkFasade.opprettJournalpost(any(), anyBoolean())).thenReturn("12234");
        when(mockDokgenService.hentDokumentInfo(any())).thenReturn(TestdataFactory.lagDokumentInfo());

        DokgenBrevbestilling brevbestilling = new DokgenBrevbestilling.Builder<>()
            .medProduserbartdokument(MELDING_FORVENTET_SAKSBEHANDLINGSTID_SOKNAD)
            .build();

        Prosessinstans prosessinstans = lagProsessinstans(behandling, brevbestilling);

        opprettJournalforBrev.utfør(prosessinstans);

        verify(mockBehandlingService).hentBehandling(anyLong());
        verify(mockDokgenService).produserBrev(any(Aktoer.class), any(DokgenBrevbestilling.class));
        verify(mockJoarkFasade).opprettJournalpost(any(), anyBoolean());
    }

    @Test
    void utførOpprettJournalforBrevTilRepresentant() {
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling);
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

        verify(mockBehandlingService).hentBehandling(anyLong());
        verify(mockDokgenService).produserBrev(any(Aktoer.class), any(DokgenBrevbestilling.class));
        verify(mockJoarkFasade).opprettJournalpost(any(), anyBoolean());
    }

    @ParameterizedTest
    @MethodSource("testparametre")
    void utfør_StorbritanniaInnvilgelseOgAttestMedUlikeParametre_korrektTittel(boolean skalHaAttest, boolean erNyVurdering, Aktoer mottaker, String forventetTittel){
        if (mottaker.erUtenlandskMyndighet()) {
            when(mockUtenlandskMyndighetService.hentUtenlandskMyndighetForInstitusjonID("1234")).thenReturn(new UtenlandskMyndighet());
        }
        else if (!FastMottakerMedOrgnr.SKATT.getOrgnr().equals(mottaker.getOrgnr())) {
            when(mockLovvalgsperiodeService.hentValidertLovvalgsperiode(anyLong())).thenReturn(lagLovvalsperiode(skalHaAttest ? UK_ART6_1 : UK_ART8_2));
        }
        Prosessinstans prosessinstans = mockStorbritannia(mottaker, erNyVurdering);

        opprettJournalforBrev.utfør(prosessinstans);

        verify(mockJoarkFasade).opprettJournalpost(opprettJournalpostCaptor.capture(), anyBoolean());

        OpprettJournalpost captured = opprettJournalpostCaptor.getValue();
        assertThat(captured.getHoveddokument().getTittel()).isEqualTo(forventetTittel);
    }

    private static Stream<Arguments> testparametre() {
        return Stream.of(
            Arguments.of(true, false, lagMottaker(BRUKER, "1234", null, null), "Vedtak om medlemskap, Attest for utsendt arbeidstaker"),
            Arguments.of(false, false, lagMottaker(BRUKER, "1234", null, null), "Vedtak om medlemskap"),
            Arguments.of(true, false, lagMottaker(ARBEIDSGIVER, null, "1234", null), "Kopi av vedtak om medlemskap, Attest for utsendt arbeidstaker"),
            Arguments.of(false, false, lagMottaker(ARBEIDSGIVER, null, "1234", null), "Kopi av vedtak om medlemskap"),
            Arguments.of(false, false, lagMottaker(TRYGDEMYNDIGHET, null, FastMottakerMedOrgnr.OrgNr.SKATTEETATEN_ORGNR.getOrgnr(), null), "Kopi av vedtak om medlemskap"),
            Arguments.of(true, false, lagMottaker(TRYGDEMYNDIGHET, null, null, "1234"), "Attest for utsendt arbeidstaker"),

            Arguments.of(true, true, lagMottaker(BRUKER, "1234", null, null), "Vedtak om medlemskap, Attest for utsendt arbeidstaker - endring"),
            Arguments.of(false, true, lagMottaker(BRUKER, "1234", null, null), "Vedtak om medlemskap - endring"),
            Arguments.of(true, true, lagMottaker(ARBEIDSGIVER, null, "1234", null), "Kopi av vedtak om medlemskap, Attest for utsendt arbeidstaker - endring"),
            Arguments.of(false, true, lagMottaker(ARBEIDSGIVER, null, "1234", null), "Kopi av vedtak om medlemskap - endring"),
            Arguments.of(false, true, lagMottaker(TRYGDEMYNDIGHET, null, FastMottakerMedOrgnr.OrgNr.SKATTEETATEN_ORGNR.getOrgnr(), null), "Kopi av vedtak om medlemskap - endring"),
            Arguments.of(true, true, lagMottaker(TRYGDEMYNDIGHET, null, null, "1234"), "Attest for utsendt arbeidstaker - endring")
        );
    }

    private Prosessinstans mockStorbritannia(Aktoer mottaker, boolean erNyVurdering) {
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(mockJoarkFasade.opprettJournalpost(any(), anyBoolean())).thenReturn("12234");
        DokumentproduksjonsInfoMapper dokumentproduksjonsInfoMapper = new DokumentproduksjonsInfoMapper(new FakeUnleash());
        when(mockDokgenService.hentDokumentInfo(any())).thenReturn(dokumentproduksjonsInfoMapper.hentDokumentproduksjonsInfo(STORBRITANNIA));

        InnvilgelseBrevbestilling brevbestilling = new InnvilgelseBrevbestilling.Builder()
            .medProduserbartdokument(STORBRITANNIA)
            .medNyVurderingBakgrunn(erNyVurdering ? Nyvurderingbakgrunner.NYE_OPPLYSNINGER.getKode() : null)
            .build();

        return lagProsessinstansMedMottaker(behandling, mottaker, brevbestilling);
    }


    @Test
    void utførOpprettJournalforMangelbrevTilBruker() {
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(mockJoarkFasade.opprettJournalpost(any(), anyBoolean())).thenReturn("12234");
        when(mockDokgenService.hentDokumentInfo(any())).thenReturn(TestdataFactory.lagDokumentInfo());

        MangelbrevBrevbestilling brevbestilling = new MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(MANGELBREV_BRUKER)
            .medManglerInfoFritekst("Mangler")
            .build();

        Prosessinstans prosessinstans = lagProsessinstans(behandling, brevbestilling);

        opprettJournalforBrev.utfør(prosessinstans);

        verify(mockBehandlingService).hentBehandling(anyLong());
        verify(mockDokgenService).produserBrev(any(Aktoer.class), any(MangelbrevBrevbestilling.class));
        verify(mockJoarkFasade).opprettJournalpost(any(), anyBoolean());
    }

    @Test
    void utførOpprettJournalforMangelbrevKopiTilBruker() {
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(mockJoarkFasade.opprettJournalpost(any(), anyBoolean())).thenReturn("12234");
        when(mockDokgenService.hentDokumentInfo(any())).thenReturn(TestdataFactory.lagDokumentInfo());

        MangelbrevBrevbestilling brevbestilling = new MangelbrevBrevbestilling.Builder()
            .medProduserbartdokument(MANGELBREV_BRUKER)
            .medManglerInfoFritekst("Mangler")
            .medBestillKopi(true)
            .build();

        Prosessinstans prosessinstans = lagProsessinstans(behandling, brevbestilling);

        opprettJournalforBrev.utfør(prosessinstans);

        verify(mockBehandlingService).hentBehandling(anyLong());
        verify(mockDokgenService).produserBrev(any(Aktoer.class), refEq(brevbestilling));
        verify(mockJoarkFasade).opprettJournalpost(any(), anyBoolean());
    }

    @Test
    void utførOpprettJournalforFritekstbrev_feilerUtentittel() {
        Behandling behandling = TestdataFactory.lagBehandling();
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling);

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
        when(mockBehandlingService.hentBehandling(anyLong())).thenReturn(behandling);
        when(mockJoarkFasade.opprettJournalpost(any(), anyBoolean())).thenReturn("12234");
        when(mockDokgenService.hentDokumentInfo(any())).thenReturn(TestdataFactory.lagDokumentInfo());

        FritekstbrevBrevbestilling brevbestilling = new FritekstbrevBrevbestilling.Builder()
            .medProduserbartdokument(GENERELT_FRITEKSTBREV_BRUKER)
            .medFritekstTittel("Tittel")
            .medFritekst("Innhold")
            .build();

        Prosessinstans prosessinstans = lagProsessinstans(behandling, brevbestilling);

        opprettJournalforBrev.utfør(prosessinstans);

        verify(mockBehandlingService).hentBehandling(anyLong());
        verify(mockDokgenService).produserBrev(any(Aktoer.class), refEq(brevbestilling));
        verify(mockJoarkFasade).opprettJournalpost(any(), anyBoolean());
    }

    private Prosessinstans lagProsessinstans(Behandling behandling, DokgenBrevbestilling brevbestilling) {
        Aktoer mottaker = lagMottaker(BRUKER, "1234", null, null);

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
        if (mottaker.getInstitusjonId() != null) prosessinstans.setData(ProsessDataKey.INSTITUSJON_ID, mottaker.getInstitusjonId());
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

    private Lovvalgsperiode lagLovvalsperiode(LovvalgBestemmelse bestemmelse) {
        var lovvalgsperiode = new Lovvalgsperiode();
        lovvalgsperiode.setBestemmelse(bestemmelse);
        return lovvalgsperiode;
    }

    private static Aktoer lagMottaker(Aktoersroller rolle, String aktørID, String orgnr, String institusjonsID) {
        Aktoer mottaker = new Aktoer();
        mottaker.setRolle(rolle);
        mottaker.setAktørId(aktørID);
        mottaker.setOrgnr(orgnr);
        mottaker.setInstitusjonId(institusjonsID);
        return mottaker;
    }
}
