package no.nav.melosys.saksflyt.agent.iv;

import java.time.LocalDate;
import java.util.*;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.*;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.brev.FastMottaker;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.dokument.DokumentSystemService;
import no.nav.melosys.service.dokument.brev.*;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerAnmodningUnntakOgAvslag;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerAvslagArbeidsgiver;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerStandard;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerVedlegg;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import static no.nav.melosys.domain.ProsessSteg.FEILET_MASKINELT;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.BRUKER;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.MYNDIGHET;
import static no.nav.melosys.domain.kodeverk.Produserbaredokumenter.*;
import static no.nav.melosys.saksflyt.brev.FastMottaker.HELFO;
import static no.nav.melosys.saksflyt.brev.FastMottaker.SKATT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class IverksettVedtakSendBrevTest {

    private final IverksettVedtakSendBrev agent;
    private static final long BEHANDLINGSID = 42L;
    private static final long IKKE_EKSISTERENDE_BEHANDLINGSID = -42L;
    private static final long BEHANDLINGSID_UTEN_PERIODER = -43L;
    private static final long BEHANDLINGSID_MED_FLERE_PERIODER = 43L;
    private static final long ART16_1_INNVILGET_BEHANDLINGSID = 44L;
    private static final long BEHANDLINGSID_UTENLANDSK_LOVVALG = 45L;
    private static final long BEHANDLINGSID_NORSK_LOVVALG_UTEN_INNVILGET_BESTEMMELSE = 46L;
    private static final long ART12_1_INNVILGET_BEHANDLINGSID = 47L;
    private static final long ART12_2_INNVILGET_BEHANDLINGSID = 48L;
    private static final long ART12_1_AVSLÅTT_BEHANDLINGSID = 49L;
    private static DokumentSystemService dokService;

    public IverksettVedtakSendBrevTest() throws Exception {
        agent = lagStegbehandler(lagBehandling(ART16_1_INNVILGET_BEHANDLINGSID));
    }

    private static IverksettVedtakSendBrev lagStegbehandler(Behandling behandling) throws Exception {
        BehandlingsresultatRepository behandlingsresultatRepo = mockBehandlingsresultatRepository();

        String saksbehandler = "Z123456";
        BrevDataA1 brevdata = new BrevDataA1();
        BrevDataVedlegg brevdataVedlegg = new BrevDataVedlegg(saksbehandler);
        brevdataVedlegg.brevDataA1 = brevdata;
        BrevDataByggerVedlegg brevDataByggerVedlegg = mock(BrevDataByggerVedlegg.class);
        when(brevDataByggerVedlegg.lag(any(), any())).thenReturn(brevdataVedlegg);
        BrevDataByggerAnmodningUnntakOgAvslag brevDataByggerAvslagYrkesaktiv = mock(BrevDataByggerAnmodningUnntakOgAvslag.class);
        BrevDataAnmodningUnntakOgAvslag brevdataAvslag = new BrevDataAnmodningUnntakOgAvslag(saksbehandler);
        when(brevDataByggerAvslagYrkesaktiv.lag(any(), any())).thenReturn(brevdataAvslag);

        BrevDataAvslagArbeidsgiver brevDataAvslagArbeidsgiver = new BrevDataAvslagArbeidsgiver(saksbehandler);
        BrevDataByggerAvslagArbeidsgiver brevDataByggerAvslagArbeidsgiver = mock(BrevDataByggerAvslagArbeidsgiver.class);
        when(brevDataByggerAvslagArbeidsgiver.lag(any(), any())).thenReturn(brevDataAvslagArbeidsgiver);

        BrevDataByggerStandard brevDataByggerStandard = mock(BrevDataByggerStandard.class);
        BrevData standardBrevData = new BrevData();
        when(brevDataByggerStandard.lag(any(), any())).thenReturn(standardBrevData);

        BrevDataByggerVelger byggerVelger = mock(BrevDataByggerVelger.class);
        when(byggerVelger.hent(eq(ANMODNING_UNNTAK))).thenReturn(brevDataByggerVedlegg);
        when(byggerVelger.hent(eq(ATTEST_A1))).thenReturn(brevDataByggerVedlegg);
        when(byggerVelger.hent(eq(INNVILGELSE_YRKESAKTIV))).thenReturn(brevDataByggerVedlegg);
        when(byggerVelger.hent(eq(AVSLAG_YRKESAKTIV))).thenReturn(brevDataByggerAvslagYrkesaktiv);
        when(byggerVelger.hent(eq(AVSLAG_ARBEIDSGIVER))).thenReturn(brevDataByggerAvslagArbeidsgiver);
        when(byggerVelger.hent(eq(INNVILGELSE_ARBEIDSGIVER))).thenReturn(brevDataByggerStandard);

        BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
        when(behandlingRepository.findWithSaksopplysningerById(eq(behandling.getId()))).thenReturn(behandling);

        Preferanse reservertMotA1Preferanse = new Preferanse(1L, Preferanse.PreferanseEnum.RESERVERT_FRA_A1);

        UtenlandskMyndighet utenlandskMyndighet = new UtenlandskMyndighet();

        UtenlandskMyndighet utenlandskMyndighetReservert = new UtenlandskMyndighet();
        utenlandskMyndighetReservert.preferanser.add(reservertMotA1Preferanse);

        UtenlandskMyndighetRepository utenlandskMyndighetRepository = mock(UtenlandskMyndighetRepository.class);
        when(utenlandskMyndighetRepository.findByLandkode(eq(Landkoder.SE))).thenReturn(utenlandskMyndighet);
        when(utenlandskMyndighetRepository.findByLandkode(eq(Landkoder.CZ))).thenReturn(utenlandskMyndighetReservert);

        dokService = Mockito.spy(lagDokumentService(byggerVelger));
        BrevBestiller brevBestiller = new BrevBestiller(dokService, byggerVelger);
        return new IverksettVedtakSendBrev(brevBestiller, behandlingRepository, behandlingsresultatRepo, utenlandskMyndighetRepository);
    }

    private static BehandlingRepository mockBehandlingRepository() {
        Fagsak fagsak = lagFagsak();
        Behandling behandling = lagBehandling(fagsak);
        BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
        List<Long> behandlingReferanser = Arrays.asList(ART16_1_INNVILGET_BEHANDLINGSID, ART12_1_INNVILGET_BEHANDLINGSID, ART12_1_AVSLÅTT_BEHANDLINGSID, ART12_2_INNVILGET_BEHANDLINGSID);
        when(behandlingRepository.findById(argThat(behandlingReferanser::contains))).thenReturn(Optional.of(behandling));
        return behandlingRepository;
    }

    private static DokumentSystemService lagDokumentService(BrevDataByggerVelger brevDataByggerVelger) {
        BehandlingRepository behandlingRepository = mockBehandlingRepository();
        FagsakRepository fagsakRepository = mock(FagsakRepository.class);
        BrevDataService brevDataService = mock(BrevDataService.class);
        DoksysFasade dokSysFasade = mock(DoksysFasade.class);
        JoarkFasade joarkFasade = mock(JoarkFasade.class);
        KontaktopplysningService kontaktopplysningService = mock(KontaktopplysningService.class);
        return spy(new DokumentSystemService(behandlingRepository, brevDataService, dokSysFasade, kontaktopplysningService, brevDataByggerVelger));
    }

    private static BehandlingsresultatRepository mockBehandlingsresultatRepository() {
        BehandlingsresultatRepository behandlingsresultatRepo = mock(BehandlingsresultatRepository.class);
        Lovvalgsperiode periode = lagLovvalgsperiodeArt16_1();
        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat(periode, Landkoder.AT, Behandlingsresultattyper.IKKE_FASTSATT);
        when(behandlingsresultatRepo.findById(BEHANDLINGSID)).thenReturn(Optional.of(behandlingsresultat));
        Lovvalgsperiode periode2 = lagLovvalgsperiode(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1, LocalDate.now().plusDays(30), Landkoder.DK, false);
        Behandlingsresultat behandlingsresultatMedFlerePerioder = lagBehandlingsresultat(new HashSet<>(Arrays.asList(periode, periode2)), null, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        assertThat(behandlingsresultatMedFlerePerioder.getLovvalgsperioder().size()).isGreaterThan(1);
        when(behandlingsresultatRepo.findById(BEHANDLINGSID_MED_FLERE_PERIODER)).thenReturn(Optional.of(behandlingsresultatMedFlerePerioder));
        Behandlingsresultat behandlingsresultatUtenPerioder = lagBehandlingsresultatUtenPerioder();
        when(behandlingsresultatRepo.findById(BEHANDLINGSID_UTEN_PERIODER)).thenReturn(Optional.of(behandlingsresultatUtenPerioder));
        Behandlingsresultat innvilgetBehandlingsResultat = lagBehandlingsresultat(periode);
        when(behandlingsresultatRepo.findById(ART16_1_INNVILGET_BEHANDLINGSID)).thenReturn(Optional.of(innvilgetBehandlingsResultat));
        Behandlingsresultat innvilgetResultat12_1 = lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1));
        when(behandlingsresultatRepo.findById(ART12_1_INNVILGET_BEHANDLINGSID)).thenReturn(Optional.of(innvilgetResultat12_1));
        Behandlingsresultat avslåttResultat12_1 = lagBehandlingsresultat(lagLovvalgsperiode(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_1, LocalDate.now(), Landkoder.HR, false));
        when(behandlingsresultatRepo.findById(ART12_1_AVSLÅTT_BEHANDLINGSID)).thenReturn(Optional.of(avslåttResultat12_1));
        Behandlingsresultat innvilgetResultat12_2 = lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(LovvalgsBestemmelser_883_2004.FO_883_2004_ART12_2));
        when(behandlingsresultatRepo.findById(ART12_2_INNVILGET_BEHANDLINGSID)).thenReturn(Optional.of(innvilgetResultat12_2));
        Behandlingsresultat utenlandskLovvalgResultat = lagBehandlingsresultat(periode, Landkoder.BE);
        when(behandlingsresultatRepo.findById(BEHANDLINGSID_UTENLANDSK_LOVVALG)).thenReturn(Optional.of(utenlandskLovvalgResultat));
        Behandlingsresultat norskLovvalgUtenInnvilgetBestemmelse = lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(LovvalgsBestemmelser_883_2004.FO_883_2004_ANNET));
        when(behandlingsresultatRepo.findById(BEHANDLINGSID_NORSK_LOVVALG_UTEN_INNVILGET_BESTEMMELSE)).thenReturn(Optional.of(norskLovvalgUtenInnvilgetBestemmelse));
        return behandlingsresultatRepo;
    }

    private static Behandling lagBehandling(Fagsak fagsak) {
        Behandling behandling = new Behandling();
        behandling.setFagsak(fagsak);
        behandling.setType(Behandlingstyper.SOEKNAD);
        return behandling;
    }

    private static Fagsak lagFagsak() {
        Fagsak fagsak = new Fagsak();
        fagsak.setGsakSaksnummer(1234L);
        fagsak.setType(Sakstyper.EU_EOS);
        Aktoer aktør = new Aktoer();
        aktør.setAktørId("1");
        aktør.setRolle(BRUKER);

        Aktoer myndighet = new Aktoer();
        myndighet.setAktørId("2");
        myndighet.setRolle(Aktoersroller.MYNDIGHET);
        myndighet.setInstitusjonId("SE:sesese123");

        fagsak.setAktører(Sets.newHashSet(aktør, myndighet));
        return fagsak;
    }

    private static Lovvalgsperiode lagLovvalgsperiodeArt16_1() {
        return lagInnvilgetLovvalgsperiode(LovvalgsBestemmelser_883_2004.FO_883_2004_ART16_1);
    }

    private static Lovvalgsperiode lagInnvilgetLovvalgsperiode(LovvalgsBestemmelser_883_2004 bestemmelse) {
        return lagLovvalgsperiode(bestemmelse, LocalDate.now(), Landkoder.NO, true);
    }

    private static Lovvalgsperiode lagLovvalgsperiode(LovvalgsBestemmelser_883_2004 bestemmelse, LocalDate fom, Landkoder land, boolean innvilget) {
        Lovvalgsperiode periode = new Lovvalgsperiode();
        periode.setFom(fom);
        periode.setTom(fom.plusDays(1));
        periode.setLovvalgsland(land);
        periode.setBestemmelse(bestemmelse);
        if (innvilget) {
            periode.setInnvilgelsesresultat(InnvilgelsesResultat.INNVILGET);
        } else {
            periode.setInnvilgelsesresultat(InnvilgelsesResultat.AVSLAATT);
        }
        return periode;
    }

    private static Behandlingsresultat lagBehandlingsresultat(Lovvalgsperiode periode) {
        return lagBehandlingsresultat(Collections.singleton(periode), Landkoder.NO, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
    }

    private static Behandlingsresultat lagBehandlingsresultat(Lovvalgsperiode periode, Landkoder land) {
        return lagBehandlingsresultat(Collections.singleton(periode), land, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
    }

    private static Behandlingsresultat lagBehandlingsresultat(Lovvalgsperiode periode, Landkoder land, Behandlingsresultattyper type) {
        return lagBehandlingsresultat(Collections.singleton(periode), land, type);
    }

    private static Behandlingsresultat lagBehandlingsresultat(Set<Lovvalgsperiode> perioder, Landkoder land, Behandlingsresultattyper type) {
        Behandlingsresultat utenlandskLovvalgResultat = new Behandlingsresultat();
        utenlandskLovvalgResultat.setLovvalgsperioder(perioder);
        utenlandskLovvalgResultat.setType(type);
        utenlandskLovvalgResultat.setFastsattAvLand(land);
        return utenlandskLovvalgResultat;
    }

    private static Behandlingsresultat lagBehandlingsresultatUtenPerioder() {
        return lagBehandlingsresultat(Collections.emptySet(), Landkoder.NO, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
    }

    @Test
    public void utfoerSteg() throws Exception {
        Prosessinstans p = new Prosessinstans();
        p.setBehandling(new Behandling());
        p.getBehandling().setId(BEHANDLINGSID);
        p.getBehandling().setType(Behandlingstyper.SOEKNAD);
        p.setType(ProsessType.IVERKSETT_VEDTAK);
        AbstraktStegBehandler instans = lagStegbehandler(lagBehandling(BEHANDLINGSID));
        instans.utførSteg(p);

        assertThat(p.getSteg()).isEqualTo(FEILET_MASKINELT);
    }

    @Test
    public final void utførSteg_MedFlereLovvalgsperioder_GirUnntak() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(BEHANDLINGSID_MED_FLERE_PERIODER);
        AbstraktStegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
    }

    @Test
    public final void utførSteg_MedIngenLovvalgsperioder_GirUnntak() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(BEHANDLINGSID_UTEN_PERIODER);
        AbstraktStegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
    }

    @Test
    public final void utførStegPåInnvilgelsesBrevBestemtAv12_1_TilSendSed() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_1_INNVILGET_BEHANDLINGSID);
        AbstraktStegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_SEND_SED);
    }

    @Test
    public void utførSteg_innvilgelses12_1_vedtakOgKopiTilSkattSendes() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_1_INNVILGET_BEHANDLINGSID);
        AbstraktStegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());

        instans.utførSteg(prosessinstans);

        verify(dokService).produserDokument(eq(INNVILGELSE_YRKESAKTIV), eq(Mottaker.av(BRUKER)), anyLong(), any());
        verify(dokService).produserDokument(eq(INNVILGELSE_YRKESAKTIV), eq(FastMottaker.av(SKATT)), anyLong(), any());
    }

    @Test
    public void utførSteg_innvilgelses12_1_kopiAvA1Sendes() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_1_INNVILGET_BEHANDLINGSID);
        AbstraktStegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());

        instans.utførSteg(prosessinstans);

        verify(dokService).produserDokument(eq(ATTEST_A1), eq(Mottaker.av(MYNDIGHET)), anyLong(), any());
    }

    @Test
    public final void utførStegPåInnvilgelsesBrevBestemtAv12_1_vedtakSendIkkeA1() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_1_INNVILGET_BEHANDLINGSID);
        prosessinstans.getBehandling().getFagsak().hentAktørMedRolleType(Aktoersroller.MYNDIGHET).setInstitusjonId("CZ:1e1");

        AbstraktStegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utførSteg(prosessinstans);

        verify(dokService).produserDokument(eq(INNVILGELSE_YRKESAKTIV), eq(Mottaker.av(BRUKER)), anyLong(), any());
        verify(dokService, never()).produserDokument(eq(ATTEST_A1), any(Mottaker.class), anyLong(), any());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_SEND_SED);
    }

    @Test
    public final void utførSteg_Avslag12_1_TilAvsluttBehandling() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_1_AVSLÅTT_BEHANDLINGSID);
        AbstraktStegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_AVSLUTT_BEHANDLING);
    }

    @Test
    public final void utførSteg_Avslag12_1_senderTilHelfoOgSkatt() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_1_AVSLÅTT_BEHANDLINGSID);
        AbstraktStegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utførSteg(prosessinstans);
        verify(dokService).produserDokument(eq(AVSLAG_YRKESAKTIV), eq(FastMottaker.av(HELFO)), anyLong(), any());
        verify(dokService).produserDokument(eq(AVSLAG_YRKESAKTIV), eq(FastMottaker.av(SKATT)), anyLong(), any());
    }

    @Test
    public final void utførStegPåInnvilgelsesBrevBestemtAv12_2_TilSendSed() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_2_INNVILGET_BEHANDLINGSID);
        AbstraktStegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_SEND_SED);
    }

    @Test
    public final void utførStegPåInnvilgelsesBrevBestemtAv16_1_TilSendSed() {
        Prosessinstans prosessinstans = lagProsessinstans(ART16_1_INNVILGET_BEHANDLINGSID);
        agent.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_SEND_SED);
    }

    @Test
    public final void utførStegPåFastsattLovvalgIUtlandetGårTilFeiletMaskinelt() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(BEHANDLINGSID_UTENLANDSK_LOVVALG);
        AbstraktStegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utførSteg(prosessinstans);        
        assertThat(prosessinstans.getSteg()).isEqualTo(FEILET_MASKINELT);
    }

    @Test
    public final void utførStegPåFastsattLovvalgINorgeUtenInnvilgetBestemmelseGårTilFeiletMaskinelt() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(BEHANDLINGSID_NORSK_LOVVALG_UTEN_INNVILGET_BESTEMMELSE);
        AbstraktStegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(FEILET_MASKINELT);
    }

    @Test
    public final void utførStegPåIkkeEksisterendeBehandlingFeiler() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(IKKE_EKSISTERENDE_BEHANDLINGSID);
        AbstraktStegBehandler instans = lagStegbehandler(lagBehandling(BEHANDLINGSID_NORSK_LOVVALG_UTEN_INNVILGET_BESTEMMELSE));
        instans.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
    }


    @Test
    public final void utførStegPåInnvilgelsesBrev_medBegrunnelsekode_oppdatererBrevdata() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_2_INNVILGET_BEHANDLINGSID);
        AbstraktStegBehandler instans = lagStegbehandler(lagBehandling(ART12_2_INNVILGET_BEHANDLINGSID));
        prosessinstans.setData(ProsessDataKey.BEGRUNNELSEKODE, Endretperioder.ENDRINGER_ARBEIDSSITUASJON);
        ArgumentCaptor<BrevData> captor = ArgumentCaptor.forClass(BrevData.class);

        instans.utførSteg(prosessinstans);

        verify(dokService, atLeastOnce()).produserDokument(any(Produserbaredokumenter.class), any(Mottaker.class), anyLong(), captor.capture());
        assertThat(captor.getValue().begrunnelseKode).isEqualTo(Endretperioder.ENDRINGER_ARBEIDSSITUASJON.getKode());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_SEND_SED);
    }

    private static Prosessinstans lagProsessinstans(long behandlingsid) {
        return lagProsessinstans(behandlingsid, ProsessType.IVERKSETT_VEDTAK);
    }

    private static Prosessinstans lagProsessinstans(long behandlingsid, ProsessType type) {
        Prosessinstans resultat = new Prosessinstans();
        Behandling behandling = lagBehandling(behandlingsid);
        resultat.setBehandling(behandling);
        resultat.setType(type);
        BrevData brevdata = new BrevData();
        resultat.setData(ProsessDataKey.BREVDATA, brevdata);
        return resultat;
    }

    private static Behandling lagBehandling(long behandlingsid) {
        Behandling behandling = new Behandling();
        behandling.setId(behandlingsid);
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setFagsak(lagFagsak());
        return behandling;
    }
}