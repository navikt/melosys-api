package no.nav.melosys.saksflyt.steg.brev;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.Avklartefaktatyper;
import no.nav.melosys.domain.kodeverk.Landkoder;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.exception.IkkeFunnetException;
import no.nav.melosys.exception.TekniskException;
import no.nav.melosys.integrasjon.doksys.DoksysFasade;
import no.nav.melosys.integrasjon.ereg.EregFasade;
import no.nav.melosys.saksflyt.brev.BrevBestiller;
import no.nav.melosys.domain.brev.FastMottaker;
import no.nav.melosys.saksflyt.steg.StegBehandler;
import no.nav.melosys.service.aktoer.KontaktopplysningService;
import no.nav.melosys.service.aktoer.UtenlandskMyndighetService;
import no.nav.melosys.service.avgift.TrygdeavgiftsberegningService;
import no.nav.melosys.service.avklartefakta.AvklarteVirksomheterService;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.*;
import no.nav.melosys.service.dokument.brev.*;
import no.nav.melosys.service.dokument.brev.bygger.*;
import no.nav.melosys.service.dokument.brev.datagrunnlag.BrevdataGrunnlagFactory;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.domain.brev.FastMottaker.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

class SendVedtaksbrevInnlandTest {
    private static final long BEHANDLINGSID = 42L;
    private static final long BEHANDLINGSID_UTEN_PERIODER = -43L;
    private static final long BEHANDLINGSID_MED_FLERE_PERIODER = 43L;
    private static final long ART16_1_INNVILGET_BEHANDLINGSID = 44L;
    private static final long ART16_1_INNVILGET_UTENLANDSK_VIRKSOMHET_BEHANDLINGSID = -44L;
    private static final long BEHANDLINGSID_NORSK_LOVVALG_UTEN_INNVILGET_BESTEMMELSE = 46L;
    private static final long ART12_1_INNVILGET_BEHANDLINGSID = 47L;
    private static final long ART12_2_INNVILGET_BEHANDLINGSID = 48L;
    private static final long ART12_1_AVSLÅTT_BEHANDLINGSID = 49L;
    private static final long BEHANDLINGSID_MANGLENDE_OPPL = 50L;
    private static final long ART13_1A_INNVILGET_BEHANDLINGSID = 51L;
    private static final long ART11_4_INNVILGET_BEHANDLINGSID = 52L;
    private static final long ART13_1B1_UTPEKING_BEHANDLINGSID = 53L;
    private static final long ART12_1_FORKORTET_PERIODE_BEHANDLINGSID = 54L;

    private static DokumentSystemService dokService;

    private static SendVedtaksbrevInnland lagStegbehandler(Behandling behandling) throws Exception {
        String saksbehandler = "Z123456";
        BrevDataA1 brevdata = new BrevDataA1();
        BrevDataVedlegg brevdataVedlegg = new BrevDataVedlegg(saksbehandler);
        brevdataVedlegg.brevDataA1 = brevdata;
        BrevDataByggerVedlegg brevDataByggerVedlegg = mock(BrevDataByggerVedlegg.class);
        when(brevDataByggerVedlegg.lag(any(), any())).thenReturn(brevdataVedlegg);
        BrevDataByggerAvslagYrkesaktiv brevDataByggerAvslagYrkesaktiv = mock(BrevDataByggerAvslagYrkesaktiv.class);
        BrevDataAvslagYrkesaktiv brevdataAvslag = new BrevDataAvslagYrkesaktiv(new BrevbestillingDto(), saksbehandler);
        when(brevDataByggerAvslagYrkesaktiv.lag(any(), any())).thenReturn(brevdataAvslag);

        BrevDataAvslagArbeidsgiver brevDataAvslagArbeidsgiver = new BrevDataAvslagArbeidsgiver(saksbehandler);
        BrevDataByggerAvslagArbeidsgiver brevDataByggerAvslagArbeidsgiver = mock(BrevDataByggerAvslagArbeidsgiver.class);
        when(brevDataByggerAvslagArbeidsgiver.lag(any(), any())).thenReturn(brevDataAvslagArbeidsgiver);

        var brevDataByggerUtpekingAnnetLand = mock(BrevDataByggerUtpekingAnnetLand.class);
        BrevDataUtpekingAnnetLand brevDataUtpekingAnnetLand = mock(BrevDataUtpekingAnnetLand.class);
        when(brevDataByggerUtpekingAnnetLand.lag(any(), any())).thenReturn(brevDataUtpekingAnnetLand);

        BrevDataByggerStandard brevDataByggerStandard = mock(BrevDataByggerStandard.class);
        BrevData standardBrevData = new BrevData();
        when(brevDataByggerStandard.lag(any(), any())).thenReturn(standardBrevData);

        BrevDataByggerVelger byggerVelger = mock(BrevDataByggerVelger.class);
        when(byggerVelger.hent(eq(ANMODNING_UNNTAK), any())).thenReturn(brevDataByggerVedlegg);
        when(byggerVelger.hent(eq(ATTEST_A1), any())).thenReturn(brevDataByggerVedlegg);
        when(byggerVelger.hent(eq(INNVILGELSE_YRKESAKTIV), any())).thenReturn(brevDataByggerVedlegg);
        when(byggerVelger.hent(eq(INNVILGELSE_YRKESAKTIV_FLERE_LAND), any())).thenReturn(brevDataByggerVedlegg);
        when(byggerVelger.hent(eq(AVSLAG_YRKESAKTIV), any())).thenReturn(brevDataByggerAvslagYrkesaktiv);
        when(byggerVelger.hent(eq(AVSLAG_ARBEIDSGIVER), any())).thenReturn(brevDataByggerAvslagArbeidsgiver);
        when(byggerVelger.hent(eq(INNVILGELSE_ARBEIDSGIVER), any())).thenReturn(brevDataByggerStandard);
        when(byggerVelger.hent(eq(AVSLAG_MANGLENDE_OPPLYSNINGER), any())).thenReturn(brevDataByggerStandard);
        when(byggerVelger.hent(eq(ORIENTERING_UTPEKING_UTLAND), any())).thenReturn(brevDataByggerUtpekingAnnetLand);

        dokService = spy(lagDokumentService(byggerVelger));
        DokumentServiceFasade dokumentServiceFasade = new DokumentServiceFasade(mock(DokumentService.class), dokService, mock(DokgenService.class),
            mock(BehandlingService.class), mock(ApplicationEventPublisher.class));
        BrevBestiller brevBestiller = new BrevBestiller(dokumentServiceFasade);

        BehandlingService behandlingService = mock(BehandlingService.class);
        when(behandlingService.hentBehandling(eq(behandling.getId()))).thenReturn(behandling);

        return new SendVedtaksbrevInnland(brevBestiller, behandlingService, mockBehandlingsresultatService());
    }

    private static BehandlingService mockBehandlingService() throws IkkeFunnetException {
        Fagsak fagsak = lagFagsak();
        Behandling behandling = lagBehandling(fagsak);
        BehandlingService behandlingService = mock(BehandlingService.class);
        List<Long> behandlingReferanser = Arrays.asList(ART16_1_INNVILGET_BEHANDLINGSID,
            ART16_1_INNVILGET_UTENLANDSK_VIRKSOMHET_BEHANDLINGSID,
            ART12_1_INNVILGET_BEHANDLINGSID,
            ART12_1_AVSLÅTT_BEHANDLINGSID,
            ART12_2_INNVILGET_BEHANDLINGSID,
            BEHANDLINGSID_MANGLENDE_OPPL,
            ART13_1A_INNVILGET_BEHANDLINGSID,
            ART13_1B1_UTPEKING_BEHANDLINGSID,
            ART11_4_INNVILGET_BEHANDLINGSID,
            ART12_1_FORKORTET_PERIODE_BEHANDLINGSID);
        when(behandlingService.hentBehandling(longThat(behandlingReferanser::contains))).thenReturn(behandling);
        return behandlingService;
    }

    private static DokumentSystemService lagDokumentService(BrevDataByggerVelger brevDataByggerVelger)
        throws TekniskException, IkkeFunnetException {
        AvklarteVirksomheterService avklarteVirksomheterService = mock(AvklarteVirksomheterService.class);
        when(avklarteVirksomheterService.hentNorskeArbeidsgivendeOrgnumre(any())).thenReturn(Set.of("123456789"));
        BehandlingService behandlingService = mockBehandlingService();
        BehandlingsresultatService behandlingsresultatService = mock(BehandlingsresultatService.class);
        BrevDataService brevDataService = mock(BrevDataService.class);
        DoksysFasade dokSysFasade = mock(DoksysFasade.class);
        UtenlandskMyndighetService utenlandskMyndighetService = mock(UtenlandskMyndighetService.class);
        when(utenlandskMyndighetService.lagUtenlandskeMyndigheterFraBehandling(any()))
            .thenReturn(Collections.singletonMap(new UtenlandskMyndighet(), new Aktoer()));
        KontaktopplysningService kontaktopplysningService = mock(KontaktopplysningService.class);
        BrevmottakerService brevmottakerService = new BrevmottakerService(kontaktopplysningService,
            avklarteVirksomheterService, utenlandskMyndighetService, behandlingsresultatService, mock(TrygdeavgiftsberegningService.class), mock(EregFasade.class));
        return spy(new DokumentSystemService(behandlingService, brevDataService, dokSysFasade,
            brevmottakerService, brevDataByggerVelger, mock(BrevdataGrunnlagFactory.class)));
    }

    private static BehandlingsresultatService mockBehandlingsresultatService() throws IkkeFunnetException {
        BehandlingsresultatService behandlingsresultatService = mock(BehandlingsresultatService.class);
        Lovvalgsperiode periode = lagLovvalgsperiodeArt16_1();
        Behandlingsresultat behandlingsresultat = lagUgyldigBehandlingsresultat(periode);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGSID)).thenReturn(behandlingsresultat);
        Lovvalgsperiode periode2 = lagLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, LocalDate.now().plusDays(30), Landkoder.DK, false);
        Behandlingsresultat behandlingsresultatMedFlerePerioder = lagBehandlingsresultat(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, new HashSet<>(Arrays.asList(periode, periode2)), null);
        assertThat(behandlingsresultatMedFlerePerioder.getLovvalgsperioder().size()).isGreaterThan(1);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGSID_MED_FLERE_PERIODER)).thenReturn(behandlingsresultatMedFlerePerioder);

        Behandlingsresultat behandlingsresultatUtenPerioder = lagBehandlingsresultatUtenPerioder(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGSID_UTEN_PERIODER)).thenReturn(behandlingsresultatUtenPerioder);

        Behandlingsresultat behandlingsresultatManglendeOppl = lagBehandlingsresultatUtenPerioder(Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGSID_MANGLENDE_OPPL)).thenReturn(behandlingsresultatManglendeOppl);

        Behandlingsresultat behandlingsresultatInnvilgetArt16 = lagBehandlingsresultat(periode);
        when(behandlingsresultatService.hentBehandlingsresultat(ART16_1_INNVILGET_BEHANDLINGSID)).thenReturn(behandlingsresultatInnvilgetArt16);

        Behandlingsresultat behandlingsresultatInnvilgetArt16MedUtenlandskKonsern = lagBehandlingsresultat(periode);
        when(behandlingsresultatService.hentBehandlingsresultat(ART16_1_INNVILGET_UTENLANDSK_VIRKSOMHET_BEHANDLINGSID)).thenReturn(behandlingsresultatInnvilgetArt16MedUtenlandskKonsern);

        Behandlingsresultat innvilgetResultat12_1 = lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1));
        when(behandlingsresultatService.hentBehandlingsresultat(ART12_1_INNVILGET_BEHANDLINGSID)).thenReturn(innvilgetResultat12_1);

        Behandlingsresultat innvilgetResultat13_1A = lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1A));
        when(behandlingsresultatService.hentBehandlingsresultat(ART13_1A_INNVILGET_BEHANDLINGSID)).thenReturn(innvilgetResultat13_1A);

        Behandlingsresultat avslåttResultat12_1 = lagBehandlingsresultat(lagLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1, LocalDate.now(), Landkoder.HR, false));
        when(behandlingsresultatService.hentBehandlingsresultat(ART12_1_AVSLÅTT_BEHANDLINGSID)).thenReturn(avslåttResultat12_1);

        Behandlingsresultat innvilgetResultat12_2 = lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_2));
        when(behandlingsresultatService.hentBehandlingsresultat(ART12_2_INNVILGET_BEHANDLINGSID)).thenReturn(innvilgetResultat12_2);

        Behandlingsresultat innvilgetResultat11_4 = lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART11_4_2));
        when(behandlingsresultatService.hentBehandlingsresultat(ART11_4_INNVILGET_BEHANDLINGSID)).thenReturn(innvilgetResultat11_4);

        Behandlingsresultat uktpekingsResultat = lagBehandlingsresultat(lagUtpekingsperiode(), lagLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1, LocalDate.now(), Landkoder.SE, true));
        when(behandlingsresultatService.hentBehandlingsresultat(ART13_1B1_UTPEKING_BEHANDLINGSID)).thenReturn(uktpekingsResultat);

        Behandlingsresultat norskLovvalgUtenInnvilgetBestemmelse = lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ANNET));
        norskLovvalgUtenInnvilgetBestemmelse.setType(Behandlingsresultattyper.IKKE_FASTSATT);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGSID_NORSK_LOVVALG_UTEN_INNVILGET_BESTEMMELSE)).thenReturn(norskLovvalgUtenInnvilgetBestemmelse);

        Behandlingsresultat endretPeriode12_1resultat = lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(Lovvalgbestemmelser_883_2004.FO_883_2004_ART12_1));
        endretPeriode12_1resultat.getAvklartefakta().add(
            new Avklartefakta(
                endretPeriode12_1resultat,
                Avklartefaktatyper.AARSAK_ENDRING_PERIODE.getKode(),
                Avklartefaktatyper.AARSAK_ENDRING_PERIODE,
                null,
                Endretperiode.ENDRINGER_ARBEIDSSITUASJON.getKode()));
        when(behandlingsresultatService.hentBehandlingsresultat(ART12_1_FORKORTET_PERIODE_BEHANDLINGSID)).thenReturn(endretPeriode12_1resultat);
        return behandlingsresultatService;
    }

    private static Behandling lagBehandling(Fagsak fagsak) {
        return lagBehandling(fagsak, 777);
    }

    private static Behandling lagBehandling(long behandlingsid) {
        return lagBehandling(null, behandlingsid);
    }

    private static Behandling lagBehandling(Fagsak fagsak, long behandlingsid) {
        Behandling behandling = new Behandling();
        behandling.setId(behandlingsid);
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(new Soeknad());
        behandling.setFagsak(fagsak != null ? fagsak : lagFagsak());
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

        fagsak.setAktører(new HashSet<>(Set.of(aktør, arbeidsgiver, myndighet)));
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

    private static Utpekingsperiode lagUtpekingsperiode() {
        Utpekingsperiode utpekingsperiode = new Utpekingsperiode();
        utpekingsperiode.setFom(LocalDate.MIN);
        utpekingsperiode.setTom(LocalDate.MIN.plusDays(1));
        utpekingsperiode.setLovvalgsland(Landkoder.PL);
        utpekingsperiode.setBestemmelse(Lovvalgbestemmelser_883_2004.FO_883_2004_ART13_1B1);
        return utpekingsperiode;
    }

    private static Behandlingsresultat lagBehandlingsresultat(Lovvalgsperiode periode) {
        return lagBehandlingsresultat(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND,
            Collections.singleton(periode),
            Landkoder.NO
        );
    }

    private static Behandlingsresultat lagUgyldigBehandlingsresultat(Lovvalgsperiode periode) {
        return lagBehandlingsresultat(Behandlingsresultattyper.IKKE_FASTSATT, Collections.singleton(periode), Landkoder.AT);
    }

    private static Behandlingsresultat lagBehandlingsresultat(Utpekingsperiode utpekingsperiode, Lovvalgsperiode lovvalgsperiode) {
        Behandlingsresultat utpekingsresultat = new Behandlingsresultat();
        utpekingsresultat.setUtpekingsperioder(Set.of(utpekingsperiode));
        utpekingsresultat.setLovvalgsperioder(Set.of(lovvalgsperiode));
        utpekingsresultat.setType(Behandlingsresultattyper.FORELOEPIG_FASTSATT_LOVVALGSLAND);
        utpekingsresultat.setFastsattAvLand(Landkoder.NO);
        return utpekingsresultat;
    }

    private static Behandlingsresultat lagBehandlingsresultat(Behandlingsresultattyper type,
                                                              Set<Lovvalgsperiode> perioder,
                                                              Landkoder land) {
        Behandlingsresultat utenlandskLovvalgResultat = new Behandlingsresultat();
        utenlandskLovvalgResultat.setLovvalgsperioder(perioder);
        utenlandskLovvalgResultat.setType(type);
        utenlandskLovvalgResultat.setFastsattAvLand(land);
        utenlandskLovvalgResultat.setVilkaarsresultater(Collections.emptySet());
        return utenlandskLovvalgResultat;
    }

    private static Behandlingsresultat lagBehandlingsresultatUtenPerioder(Behandlingsresultattyper behandlingstype) {
        return lagBehandlingsresultat(behandlingstype, Collections.emptySet(), Landkoder.NO);
    }

    @Test
    final void utfør_medFlereLovvalgsperioder_girUnntak() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(BEHANDLINGSID_MED_FLERE_PERIODER);
        StegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> instans.utfør(prosessinstans))
            .withMessageContaining("Flere enn en lovvalgsperiode er ikke støttet");
    }

    @Test
    final void utfør_avslagManglendeOppl_bestillerAvslagManglendeOppl() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(BEHANDLINGSID_MANGLENDE_OPPL);
        StegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utfør(prosessinstans);
        verify(dokService).produserDokument(eq(AVSLAG_MANGLENDE_OPPLYSNINGER), eq(Mottaker.av(BRUKER)), anyLong(), any());
        verify(dokService).produserDokument(eq(AVSLAG_MANGLENDE_OPPLYSNINGER), eq(Mottaker.av(ARBEIDSGIVER)), anyLong(), any());
    }

    @Test
    final void utfør_avslagManglendeOppl_senderIkkeTilSkattOgHelfo() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(BEHANDLINGSID_MANGLENDE_OPPL);
        StegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utfør(prosessinstans);
        verify(dokService, never()).produserDokument(eq(AVSLAG_MANGLENDE_OPPLYSNINGER), eq(FastMottaker.av(SKATT)), anyLong(), any());
        verify(dokService, never()).produserDokument(eq(AVSLAG_MANGLENDE_OPPLYSNINGER), eq(FastMottaker.av(HELFO)), anyLong(), any());
    }

    @Test
    final void utfør_utenPeriode_feiler() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(BEHANDLINGSID_UTEN_PERIODER);
        StegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        assertThatExceptionOfType(NoSuchElementException.class)
            .isThrownBy(() -> instans.utfør(prosessinstans))
            .withMessageContaining("Ingen lovvalgsperiode finnes");
    }

    @Test
    final void utfør_påInnvilgelsesBrevBestemtAv12_1_tilSendSed() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_1_INNVILGET_BEHANDLINGSID);
        StegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utfør(prosessinstans);
    }

    @Test
    void utfør_innvilgelses12_1_vedtakOgKopiTilSkattSendes() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_1_INNVILGET_BEHANDLINGSID);
        StegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());

        instans.utfør(prosessinstans);

        verify(dokService).produserDokument(eq(INNVILGELSE_YRKESAKTIV), eq(Mottaker.av(BRUKER)), anyLong(), any());
        verify(dokService).produserDokument(eq(INNVILGELSE_YRKESAKTIV), eq(FastMottaker.av(SKATT)), anyLong(), any());
    }

    @Test
    final void utfør_innvilgelses11_4_senderIkkeOrienteringTilArbeidsgiver() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART11_4_INNVILGET_BEHANDLINGSID);

        StegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utfør(prosessinstans);

        verify(dokService).produserDokument(eq(INNVILGELSE_YRKESAKTIV), eq(Mottaker.av(BRUKER)), anyLong(), any());
        verify(dokService, never()).produserDokument(eq(INNVILGELSE_ARBEIDSGIVER), any(), anyLong(), any());
    }

    @Test
    void utfør_innvilgelses13_1A_vedtakOgKopiTilSkattSendes() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART13_1A_INNVILGET_BEHANDLINGSID);
        StegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());

        instans.utfør(prosessinstans);

        verify(dokService).produserDokument(eq(INNVILGELSE_YRKESAKTIV_FLERE_LAND), eq(Mottaker.av(BRUKER)), anyLong(), any());
        verify(dokService).produserDokument(eq(INNVILGELSE_YRKESAKTIV_FLERE_LAND), eq(FastMottaker.av(SKATT)), anyLong(), any());
    }

    @Test
    void utfør_utpeking_senderOrienteringsbrev() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART13_1B1_UTPEKING_BEHANDLINGSID);
        StegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utfør(prosessinstans);

        verify(dokService).produserDokument(eq(ORIENTERING_UTPEKING_UTLAND), eq(Mottaker.av(BRUKER)), anyLong(), any());
    }

    @Test
    void utfør_innvilgelses13_1A_senderIkkeInnvilgelseTilArbeidsgiver() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART13_1A_INNVILGET_BEHANDLINGSID);
        StegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());

        instans.utfør(prosessinstans);

        verify(dokService, never()).produserDokument(eq(INNVILGELSE_ARBEIDSGIVER), any(), anyLong(), any());
    }

    @Test
    void utfør_innvilgelsesMedUtenlandskForetak_senderBrevTilStatligSkatteoppkreving() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART13_1A_INNVILGET_BEHANDLINGSID);
        StegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        ForetakUtland arbeidsgiverUtland = new ForetakUtland();
        arbeidsgiverUtland.selvstendigNæringsvirksomhet = Boolean.FALSE;
        prosessinstans.getBehandling().getBehandlingsgrunnlag().getBehandlingsgrunnlagdata()
            .foretakUtland.add(arbeidsgiverUtland);
        instans.utfør(prosessinstans);

        verify(dokService).produserDokument(eq(INNVILGELSE_YRKESAKTIV_FLERE_LAND), eq(FastMottaker.av(STATLIG_SKATTEOPPKREVING)), anyLong(), any());
    }

    @Test
    void utfør_innvilgelse161MedUtenlandskSelvstendigArbeid_senderIkkeBrevTilStatligSkatteoppkreving() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART16_1_INNVILGET_BEHANDLINGSID);
        StegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        ForetakUtland utenlandskSelvstendigVirksomhet = new ForetakUtland();
        utenlandskSelvstendigVirksomhet.selvstendigNæringsvirksomhet = Boolean.TRUE;
        prosessinstans.getBehandling().getBehandlingsgrunnlag().getBehandlingsgrunnlagdata()
            .foretakUtland.add(utenlandskSelvstendigVirksomhet);

        instans.utfør(prosessinstans);

        verify(dokService, never()).produserDokument(any(), eq(FastMottaker.av(STATLIG_SKATTEOPPKREVING)), anyLong(), any());
    }

    @Test
    void utfør_innvilgelses12_senderIkkeBrevTilStatligSkatteoppkreving() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_1_INNVILGET_BEHANDLINGSID);
        prosessinstans.getBehandling().getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().foretakUtland.add(new ForetakUtland());
        StegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utfør(prosessinstans);

        verify(dokService, never()).produserDokument(any(), eq(FastMottaker.av(STATLIG_SKATTEOPPKREVING)), anyLong(), any());
    }

    @Test
    void utfør_innvilgelses12_senderInnvilgelseTilArbeidsgiver() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_1_INNVILGET_BEHANDLINGSID);
        StegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());

        instans.utfør(prosessinstans);

        verify(dokService).produserDokument(eq(INNVILGELSE_ARBEIDSGIVER), any(), anyLong(), any());
    }

    @Test
    final void utfør_avslag12_1_tilOppdaterResultat() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_1_AVSLÅTT_BEHANDLINGSID);
        StegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utfør(prosessinstans);
    }

    @Test
    final void utfør_avslagMedArbeidsgiver_senderTilArbeidsgiver() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_1_AVSLÅTT_BEHANDLINGSID);
        Behandling behandling = prosessinstans.getBehandling();
        Aktoer arbeidsgiver = new Aktoer();
        arbeidsgiver.setRolle(ARBEIDSGIVER);
        arbeidsgiver.setOrgnr("123456789");
        behandling.getFagsak().getAktører().add(arbeidsgiver);
        StegBehandler instans = lagStegbehandler(behandling);
        instans.utfør(prosessinstans);
        verify(dokService).produserDokument(eq(AVSLAG_ARBEIDSGIVER), eq(Mottaker.av(ARBEIDSGIVER)), anyLong(), any());
    }

    @Test
    final void utfør_avslag12_1_senderTilHelfoOgSkatt() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_1_AVSLÅTT_BEHANDLINGSID);
        StegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utfør(prosessinstans);
        verify(dokService).produserDokument(eq(AVSLAG_YRKESAKTIV), eq(FastMottaker.av(HELFO)), anyLong(), any());
        verify(dokService).produserDokument(eq(AVSLAG_YRKESAKTIV), eq(FastMottaker.av(SKATT)), anyLong(), any());
    }

    @Test
    final void utfør_PåInnvilgelsesBrevBestemtAv12_2_tilSendSed() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_2_INNVILGET_BEHANDLINGSID);
        StegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        instans.utfør(prosessinstans);
    }

    @Test
    final void utfør_PåInnvilgelsesBrevBestemtAv16_1_tilSendSed() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART16_1_INNVILGET_BEHANDLINGSID);
        lagStegbehandler(prosessinstans.getBehandling()).utfør(prosessinstans);
    }

    @Test
    final void utfør_PåFastsattLovvalgINorgeUtenInnvilgetBestemmelseGårTilFeiletMaskinelt() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(BEHANDLINGSID_NORSK_LOVVALG_UTEN_INNVILGET_BESTEMMELSE);
        StegBehandler instans = lagStegbehandler(prosessinstans.getBehandling());
        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> instans.utfør(prosessinstans))
            .withMessageContaining("Vedtaksbrev kan ikke sendes for behandling");
    }

    @Test
    final void utfør_PåInnvilgelsesBrev_medBegrunnelsekode_oppdatererBrevdata() throws Exception {
        Prosessinstans prosessinstans = lagProsessinstans(ART12_1_FORKORTET_PERIODE_BEHANDLINGSID);
        StegBehandler instans = lagStegbehandler(lagBehandling(ART12_1_FORKORTET_PERIODE_BEHANDLINGSID));
        ArgumentCaptor<DoksysBrevbestilling> captor = ArgumentCaptor.forClass(DoksysBrevbestilling.class);

        instans.utfør(prosessinstans);

        verify(dokService, atLeastOnce()).produserDokument(any(Produserbaredokumenter.class), eq(Mottaker.av(BRUKER)), anyLong(), captor.capture());
        assertThat(captor.getValue().getBegrunnelseKode()).isEqualTo(Endretperiode.ENDRINGER_ARBEIDSSITUASJON.getKode());
    }

    private static Prosessinstans lagProsessinstans(long behandlingsid) {
        Prosessinstans resultat = new Prosessinstans();
        Behandling behandling = lagBehandling(behandlingsid);
        resultat.setBehandling(behandling);
        resultat.setType(ProsessType.IVERKSETT_VEDTAK);
        BrevData brevdata = new BrevData();
        resultat.setData(ProsessDataKey.BREVDATA, brevdata);
        return resultat;
    }
}