package no.nav.melosys.saksflyt.steg.iv;

import java.time.LocalDate;
import java.util.*;

import com.google.common.collect.Sets;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.brev.Brevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.saksflyt.brev.FastMottaker;
import no.nav.melosys.saksflyt.steg.AbstraktStegBehandler;
import no.nav.melosys.service.BehandlingService;
import no.nav.melosys.service.BehandlingsresultatService;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.dokument.BrevmottakerService;
import no.nav.melosys.service.dokument.DokumentSystemService;
import no.nav.melosys.service.dokument.brev.*;
import no.nav.melosys.service.dokument.brev.bygger.*;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevdataGrunnlagFactory;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static no.nav.melosys.domain.ProsessSteg.FEILET_MASKINELT;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.saksflyt.brev.FastMottaker.HELFO;
import static no.nav.melosys.saksflyt.brev.FastMottaker.SKATT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class SendVedtaksbrevInnlandTest {

    private final SendVedtaksbrevInnland agent;
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
    private static final long BEHANDLINGSID_MANGLENDE_OPPL = 50L;
    private static final long ART13_1A_INNVILGET_BEHANDLINGSID = 51L;
    private static DokumentSystemService dokService;

    public SendVedtaksbrevInnlandTest() throws Exception {
        agent = lagStegbehandler(lagBehandling(ART16_1_INNVILGET_BEHANDLINGSID));
    }

    private static SendVedtaksbrevInnland lagStegbehandler(Behandling behandling) throws Exception {
        BehandlingsresultatService behandlingsresultatService = mockBehandlingsresultatService();

        String saksbehandler = "Z123456";
        BrevDataA1 brevdata = new BrevDataA1();
        BrevDataVedlegg brevdataVedlegg = new BrevDataVedlegg(saksbehandler);
        brevdataVedlegg.brevDataA1 = brevdata;
        BrevDataByggerVedlegg brevDataByggerVedlegg = mock(BrevDataByggerVedlegg.class);
        when(brevDataByggerVedlegg.lag(any(), any())).thenReturn(brevdataVedlegg);
        BrevDataByggerAvslagYrkesaktiv brevDataByggerAvslagYrkesaktiv = mock(BrevDataByggerAvslagYrkesaktiv.class);
        BrevDataAvslagYrkesaktiv brevdataAvslag = new BrevDataAvslagYrkesaktiv(saksbehandler);
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
        when(byggerVelger.hent(eq(INNVILGELSE_YRKESAKTIV_FLERE_LAND))).thenReturn(brevDataByggerVedlegg);
        when(byggerVelger.hent(eq(AVSLAG_YRKESAKTIV))).thenReturn(brevDataByggerAvslagYrkesaktiv);
        when(byggerVelger.hent(eq(AVSLAG_ARBEIDSGIVER))).thenReturn(brevDataByggerAvslagArbeidsgiver);
        when(byggerVelger.hent(eq(INNVILGELSE_ARBEIDSGIVER))).thenReturn(brevDataByggerStandard);
        when(byggerVelger.hent(eq(AVSLAG_MANGLENDE_OPPLYSNINGER))).thenReturn(brevDataByggerStandard);

        BehandlingService behandlingService = mock(BehandlingService.class);
        when(behandlingService.hentBehandling(eq(behandling.getId()))).thenReturn(behandling);

        dokService = spy(lagDokumentService(byggerVelger));
        BrevBestiller brevBestiller = new BrevBestiller(dokService);
        return new SendVedtaksbrevInnland(brevBestiller, behandlingService, behandlingsresultatService);
    }

    private static BehandlingService mockBehandlingService() throws IkkeFunnetException {
        Fagsak fagsak = lagFagsak();
        Behandling behandling = lagBehandling(fagsak);
        BehandlingService behandlingService = mock(BehandlingService.class);
        List<Long> behandlingReferanser = Arrays.asList(ART16_1_INNVILGET_BEHANDLINGSID, ART12_1_INNVILGET_BEHANDLINGSID,
            ART12_1_AVSLÅTT_BEHANDLINGSID, ART12_2_INNVILGET_BEHANDLINGSID, BEHANDLINGSID_MANGLENDE_OPPL, ART13_1A_INNVILGET_BEHANDLINGSID);
        when(behandlingService.hentBehandling(longThat(behandlingReferanser::contains))).thenReturn(behandling);
        return behandlingService;
    }

    private static DokumentSystemService lagDokumentService(BrevDataByggerVelger brevDataByggerVelger) throws TekniskException, IkkeFunnetException {
        AvklarteVirksomheterService avklarteVirksomheterService = mock(AvklarteVirksomheterService.class);
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(any())).thenReturn(Sets.newHashSet("123456789"));
        BehandlingService behandlingService = mockBehandlingService();
        BrevDataService brevDataService = mock(BrevDataService.class);
        DoksysFasade dokSysFasade = mock(DoksysFasade.class);
        UtenlandskMyndighetService utenlandskMyndighetService = mock(UtenlandskMyndighetService.class);
        when(utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(any())).thenReturn(Collections.singletonMap(new UtenlandskMyndighet(), new Aktoer()));
        KontaktopplysningService kontaktopplysningService = mock(KontaktopplysningService.class);
        BrevmottakerService brevmottakerService = new BrevmottakerService(kontaktopplysningService, avklarteVirksomheterService, utenlandskMyndighetService);
        return spy(new DokumentSystemService(behandlingService, brevDataService, dokSysFasade, brevmottakerService, brevDataByggerVelger, mock(BrevdataGrunnlagFactory.class)));
    }

    private static BehandlingsresultatService mockBehandlingsresultatService() throws IkkeFunnetException {
        BehandlingsresultatService behandlingsresultatService = mock(BehandlingsresultatService.class);
        Lovvalgsperiode periode = lagLovvalgsperiodeArt16_1();
        Behandlingsresultat behandlingsresultat = lagBehandlingsresultat(periode, Landkoder.AT, Behandlingsresultattyper.IKKE_FASTSATT);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGSID)).thenReturn(behandlingsresultat);
        Lovvalgsperiode periode2 = lagLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, LocalDate.now().plusDays(30), Landkoder.DK, false);
        Behandlingsresultat behandlingsresultatMedFlerePerioder = lagBehandlingsresultat(new HashSet<>(Arrays.asList(periode, periode2)), null, Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        assertThat(behandlingsresultatMedFlerePerioder.getLovvalgsperioder().size()).isGreaterThan(1);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGSID_MED_FLERE_PERIODER)).thenReturn(behandlingsresultatMedFlerePerioder);

        Behandlingsresultat behandlingsresultatUtenPerioder = lagBehandlingsresultatUtenPerioder(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGSID_UTEN_PERIODER)).thenReturn(behandlingsresultatUtenPerioder);

        Behandlingsresultat behandlingsresultatManglendeOppl = lagBehandlingsresultatUtenPerioder(Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGSID_MANGLENDE_OPPL)).thenReturn(behandlingsresultatManglendeOppl);

        Behandlingsresultat innvilgetBehandlingsResultat = lagBehandlingsresultat(periode);
        when(behandlingsresultatService.hentBehandlingsresultat(ART16_1_INNVILGET_BEHANDLINGSID)).thenReturn(innvilgetBehandlingsResultat);

        Behandlingsresultat innvilgetResultat12_1 = lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1));
        when(behandlingsresultatService.hentBehandlingsresultat(ART12_1_INNVILGET_BEHANDLINGSID)).thenReturn(innvilgetResultat12_1);

        Behandlingsresultat innvilgetResultat13_1A = lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A));
        when(behandlingsresultatService.hentBehandlingsresultat(ART13_1A_INNVILGET_BEHANDLINGSID)).thenReturn(innvilgetResultat13_1A);

        Behandlingsresultat avslåttResultat12_1 = lagBehandlingsresultat(lagLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, LocalDate.now(), Landkoder.HR, false));
        when(behandlingsresultatService.hentBehandlingsresultat(ART12_1_AVSLÅTT_BEHANDLINGSID)).thenReturn(avslåttResultat12_1);

        Behandlingsresultat innvilgetResultat12_2 = lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2));
        when(behandlingsresultatService.hentBehandlingsresultat(ART12_2_INNVILGET_BEHANDLINGSID)).thenReturn(innvilgetResultat12_2);

        Behandlingsresultat utenlandskLovvalgResultat = lagBehandlingsresultat(periode, Landkoder.BE);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGSID_UTENLANDSK_LOVVALG)).thenReturn(utenlandskLovvalgResultat);

        Behandlingsresultat norskLovvalgUtenInnvilgetBestemmelse = lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ANNET));
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGSID_NORSK_LOVVALG_UTEN_INNVILGET_BESTEMMELSE)).thenReturn(norskLovvalgUtenInnvilgetBestemmelse);

        return behandlingsresultatService;
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
        myndighet.setRolle(MYNDIGHET);
        myndighet.setInstitusjonId("SE:sesese123");

        Aktoer arbeidsgiver = new Aktoer();
        arbeidsgiver.setRolle(ARBEIDSGIVER);
        arbeidsgiver.setOrgnr("123456789");

        fagsak.setAktører(Sets.newHashSet(aktør, arbeidsgiver, myndighet));
        return fagsak;
    }

    private static Lovvalgsperiode lagLovvalgsperiodeArt16_1() {
        return lagInnvilgetLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART16_1);
    }

    private static Lovvalgsperiode lagInnvilgetLovvalgsperiode(Lovvalgbestemmelser_883_2004 bestemmelse) {
        return lagLovvalgsperiode(bestemmelse, LocalDate.now(), Landkoder.NO, true);
    }

    private static Lovvalgsperiode lagLovvalgsperiode(Lovvalgbestemmelser_883_2004 bestemmelse, LocalDate fom, Landkoder land, boolean innvilget) {
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

    private static Behandlingsresultat lagBehandlingsresultatUtenPerioder(Behandlingsresultattyper behandlingstype) {
        return lagBehandlingsresultat(Collections.emptySet(), Landkoder.NO, behandlingstype);
    }

    @Test
    public void utførSteg() throws Exception {
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
    public final void utførSteg_medFlereLovvalgsperioder_girUnntak() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(BEHANDLINGSID_MED_FLERE_PERIODER);
        AbstraktStegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(FEILET_MASKINELT);
    }

    @Test
    public final void utførSteg_avslagManglendeOppl_bestillerAvslagManglendeOppl() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(BEHANDLINGSID_MANGLENDE_OPPL);
        AbstraktStegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utførSteg(prosessinstans);
        verify(dokService).produserDokument(eq(AVSLAG_MANGLENDE_OPPLYSNINGER), eq(Mottaker.av(BRUKER)), anyLong(), any());
        verify(dokService).produserDokument(eq(AVSLAG_MANGLENDE_OPPLYSNINGER), eq(Mottaker.av(ARBEIDSGIVER)), anyLong(), any());
    }

    @Test
    public final void utførSteg_sendBrev_girUnntak() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(BEHANDLINGSID_UTEN_PERIODER);
        AbstraktStegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(FEILET_MASKINELT);
    }

    @Test
    public final void utførSteg_påInnvilgelsesBrevBestemtAv12_1_tilSendSed() throws Exception {
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
    public final void utførStegPåInnvilgelsesBrevBestemtAv12_1_vedtakSendIkkeA1() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_1_INNVILGET_BEHANDLINGSID);
        prosessinstans.getBehandling().getFagsak().hentMyndigheter().iterator().next().setInstitusjonId("CZ:1e1");

        AbstraktStegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utførSteg(prosessinstans);

        verify(dokService).produserDokument(eq(INNVILGELSE_YRKESAKTIV), eq(Mottaker.av(BRUKER)), anyLong(), any());
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_SEND_SED);
    }

    @Test
    public void utførSteg_innvilgelses13_1A_vedtakOgKopiTilSkattSendes() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART13_1A_INNVILGET_BEHANDLINGSID);
        AbstraktStegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());

        instans.utførSteg(prosessinstans);

        verify(dokService).produserDokument(eq(INNVILGELSE_YRKESAKTIV_FLERE_LAND), eq(Mottaker.av(BRUKER)), anyLong(), any());
        verify(dokService).produserDokument(eq(INNVILGELSE_YRKESAKTIV_FLERE_LAND), eq(FastMottaker.av(SKATT)), anyLong(), any());
    }

    @Test
    public void utførSteg_innvilgelses13_1A_senderIkkeInnvilgelseTilArbeidsgiver() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART13_1A_INNVILGET_BEHANDLINGSID);
        AbstraktStegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());

        instans.utførSteg(prosessinstans);

        verify(dokService, never()).produserDokument(eq(INNVILGELSE_ARBEIDSGIVER), any(), anyLong(), any());
    }

    @Test
    public void utførSteg_innvilgelses12_senderInnvilgelseTilArbeidsgiver() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_1_INNVILGET_BEHANDLINGSID);
        AbstraktStegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());

        instans.utførSteg(prosessinstans);

        verify(dokService).produserDokument(eq(INNVILGELSE_ARBEIDSGIVER), any(), anyLong(), any());
    }

    @Test
    public final void utførSteg_avslag12_1_tilAvsluttBehandling() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_1_AVSLÅTT_BEHANDLINGSID);
        AbstraktStegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_AVSLUTT_BEHANDLING);
    }

    @Test
    public final void utførSteg_avslagMedArbeidsgiver_senderTilArbeidsgiver() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_1_AVSLÅTT_BEHANDLINGSID);
        Behandling behandling = prosessinstans.getBehandling();
        Aktoer arbeidsgiver = new Aktoer();
        arbeidsgiver.setRolle(ARBEIDSGIVER);
        arbeidsgiver.setOrgnr("123456789");
        behandling.getFagsak().getAktører().add(arbeidsgiver);
        AbstraktStegBehandler instans = lagStegbehandler(behandling);
        instans.utførSteg(prosessinstans);
        verify(dokService).produserDokument(eq(AVSLAG_ARBEIDSGIVER), eq(Mottaker.av(ARBEIDSGIVER)), anyLong(), any());
    }

    @Test
    public final void utførSteg_avslag12_1_senderTilHelfoOgSkatt() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_1_AVSLÅTT_BEHANDLINGSID);
        AbstraktStegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utførSteg(prosessinstans);
        verify(dokService).produserDokument(eq(AVSLAG_YRKESAKTIV), eq(FastMottaker.av(HELFO)), anyLong(), any());
        verify(dokService).produserDokument(eq(AVSLAG_YRKESAKTIV), eq(FastMottaker.av(SKATT)), anyLong(), any());
    }

    @Test
    public final void utførStegPåInnvilgelsesBrevBestemtAv12_2_tilSendSed() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_2_INNVILGET_BEHANDLINGSID);
        AbstraktStegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utførSteg(prosessinstans);
        assertThat(prosessinstans.getSteg()).isEqualTo(ProsessSteg.IV_SEND_SED);
    }

    @Test
    public final void utførStegPåInnvilgelsesBrevBestemtAv16_1_tilSendSed() {
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
        assertThat(prosessinstans.getSteg()).isEqualTo(FEILET_MASKINELT);
    }


    @Test
    public final void utførStegPåInnvilgelsesBrev_medBegrunnelsekode_oppdatererBrevdata() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_2_INNVILGET_BEHANDLINGSID);
        AbstraktStegBehandler instans = lagStegbehandler(lagBehandling(ART12_2_INNVILGET_BEHANDLINGSID));
        prosessinstans.setData(ProsessDataKey.BEGRUNNELSEKODE, Endretperiode.ENDRINGER_ARBEIDSSITUASJON);
        ArgumentCaptor<Brevbestilling> captor = ArgumentCaptor.forClass(Brevbestilling.class);

        instans.utførSteg(prosessinstans);

        verify(dokService, atLeastOnce()).produserDokument(any(Produserbaredokumenter.class), eq(Mottaker.av(BRUKER)), anyLong(), captor.capture());
        assertThat(captor.getValue().getBegrunnelseKode()).isEqualTo(Endretperiode.ENDRINGER_ARBEIDSSITUASJON.getKode());
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