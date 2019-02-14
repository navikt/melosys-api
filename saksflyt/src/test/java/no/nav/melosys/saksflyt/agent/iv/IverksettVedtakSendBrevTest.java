package no.nav.melosys.saksflyt.agent.iv;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.integrasjon.doksys.DokSysFasade;
import no.nav.melosys.integrasjon.joark.JoarkFasade;
import no.nav.melosys.repository.BehandlingRepository;
import no.nav.melosys.repository.BehandlingsresultatRepository;
import no.nav.melosys.repository.FagsakRepository;
import no.nav.melosys.saksflyt.agent.AbstraktStegBehandler;
import no.nav.melosys.service.dokument.DokumentSystemService;
import no.nav.melosys.service.dokument.brev.*;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerAnmodningUnntakOgAvslag;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerAvslagArbeidsgiver;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerStandard;
import no.nav.melosys.service.dokument.brev.bygger.BrevDataByggerVedlegg;
import org.junit.Test;

import static no.nav.melosys.domain.ProsessSteg.FEILET_MASKINELT;
import static no.nav.melosys.domain.kodeverk.Produserbaredokumenter.AVSLAG_ARBEIDSGIVER;
import static no.nav.melosys.domain.kodeverk.Produserbaredokumenter.AVSLAG_YRKESAKTIV;
import static no.nav.melosys.domain.kodeverk.Produserbaredokumenter.INNVILGELSE_YRKESAKTIV;
import static no.nav.melosys.domain.kodeverk.Produserbaredokumenter.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
        when(byggerVelger.hent(eq(INNVILGELSE_YRKESAKTIV))).thenReturn(brevDataByggerVedlegg);
        when(byggerVelger.hent(eq(AVSLAG_YRKESAKTIV))).thenReturn(brevDataByggerAvslagYrkesaktiv);
        when(byggerVelger.hent(eq(AVSLAG_ARBEIDSGIVER))).thenReturn(brevDataByggerAvslagArbeidsgiver);

        BehandlingRepository behandlingRepository = mock(BehandlingRepository.class);
        when(behandlingRepository.findWithSaksopplysningerById(eq(behandling.getId()))).thenReturn(behandling);

        DokumentSystemService dokService = lagDokumentService(byggerVelger);
        return new IverksettVedtakSendBrev(dokService, byggerVelger, behandlingRepository, behandlingsresultatRepo);
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
        DokSysFasade dokSysFasade = mock(DokSysFasade.class);
        JoarkFasade joarkFasade = mock(JoarkFasade.class);
        return new DokumentSystemService(behandlingRepository, fagsakRepository,
                brevDataService, dokSysFasade, joarkFasade, brevDataByggerVelger);
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
        aktør.setRolle(Aktoersroller.BRUKER);
        fagsak.setAktører(Collections.singleton(aktør));
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
        AbstraktStegBehandler instans = lagStegbehandler(lagBehandling(BEHANDLINGSID_MED_FLERE_PERIODER));
        instans.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
    }

    @Test
    public final void utførSteg_MedIngenLovvalgsperioder_GirUnntak() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(BEHANDLINGSID_UTEN_PERIODER);
        AbstraktStegBehandler instans = lagStegbehandler(lagBehandling(BEHANDLINGSID_UTEN_PERIODER));
        instans.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
    }

    @Test
    public final void utførStegPåInnvilgelsesBrevBestemtAv12_1_TilSendSed() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_1_INNVILGET_BEHANDLINGSID);
        AbstraktStegBehandler instans = lagStegbehandler(lagBehandling(ART12_1_INNVILGET_BEHANDLINGSID));
        instans.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_SEND_SED);
    }

    @Test
    public final void utførSteg_Avslag12_1_TilAvsluttBehandling() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_1_AVSLÅTT_BEHANDLINGSID);
        AbstraktStegBehandler instans = lagStegbehandler(lagBehandling(ART12_1_AVSLÅTT_BEHANDLINGSID));
        instans.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_AVSLUTT_BEHANDLING);
    }    

    @Test
    public final void utførStegPåInnvilgelsesBrevBestemtAv12_2_TilSendSed() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_2_INNVILGET_BEHANDLINGSID);
        AbstraktStegBehandler instans = lagStegbehandler(lagBehandling(ART12_2_INNVILGET_BEHANDLINGSID));
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
        AbstraktStegBehandler instans = lagStegbehandler(lagBehandling(BEHANDLINGSID_UTENLANDSK_LOVVALG));
        instans.utførSteg(prosessinstans);        
        assertThat(prosessinstans.getSteg()).isEqualTo(FEILET_MASKINELT);
    }

    @Test
    public final void utførStegPåFastsattLovvalgINorgeUtenInnvilgetBestemmelseGårTilFeiletMaskinelt() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(BEHANDLINGSID_NORSK_LOVVALG_UTEN_INNVILGET_BESTEMMELSE);
        AbstraktStegBehandler instans = lagStegbehandler(lagBehandling(BEHANDLINGSID_NORSK_LOVVALG_UTEN_INNVILGET_BESTEMMELSE));
        instans.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(FEILET_MASKINELT);
    }

    @Test
    public final void utførStegPåUkjentProsesstypeFeiler() {
        Prosessinstans prosessinstans = lagProsessinstans(ART16_1_INNVILGET_BEHANDLINGSID, ProsessType.JFR_KNYTT);
        agent.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);
    }

    @Test
    public final void utførStegPåIkkeEksisterendeBehandlingFeiler() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(IKKE_EKSISTERENDE_BEHANDLINGSID);
        AbstraktStegBehandler instans = lagStegbehandler(lagBehandling(BEHANDLINGSID_NORSK_LOVVALG_UTEN_INNVILGET_BESTEMMELSE));
        instans.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.FEILET_MASKINELT);

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
        brevdata.mottaker = Aktoersroller.BRUKER;
        resultat.setData(ProsessDataKey.BREVDATA, brevdata);
        return resultat;
    }

    private static Behandling lagBehandling(long behandlingsid) {
        Behandling behandling = new Behandling();
        behandling.setId(behandlingsid);
        behandling.setType(Behandlingstyper.SOEKNAD);
        return behandling;
    }
}