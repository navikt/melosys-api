package no.nav.melosys.saksflyt.steg.brev;

import java.time.LocalDate;
import java.util.*;

import no.nav.melosys.domain.*;
import no.nav.melosys.domain.avklartefakta.Avklartefakta;
import no.nav.melosys.domain.behandlingsgrunnlag.Behandlingsgrunnlag;
import no.nav.melosys.domain.behandlingsgrunnlag.Soeknad;
import no.nav.melosys.domain.behandlingsgrunnlag.data.ForetakUtland;
import no.nav.melosys.domain.brev.DoksysBrevbestilling;
import no.nav.melosys.domain.brev.FastMottakerMedOrgnr;
import no.nav.melosys.domain.brev.Mottaker;
import no.nav.melosys.domain.kodeverk.*;
import no.nav.melosys.domain.kodeverk.begrunnelser.Endretperiode;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsresultattyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004;
import no.nav.melosys.domain.saksflyt.ProsessDataKey;
import no.nav.melosys.domain.saksflyt.ProsessType;
import no.nav.melosys.domain.saksflyt.Prosessinstans;
import no.nav.melosys.exception.FunksjonellException;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.dokument.brev.BrevData;
import no.nav.melosys.service.saksflyt.ProsessinstansService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.brev.FastMottakerMedOrgnr.*;
import static no.nav.melosys.domain.kodeverk.Aktoersroller.*;
import static no.nav.melosys.domain.kodeverk.brev.Produserbaredokumenter.*;
import static no.nav.melosys.domain.kodeverk.lovvalgsbestemmelser.Lovvalgbestemmelser_883_2004.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SendVedtaksbrevInnlandTest {
    private static final long BEHANDLINGID = 1L;

    @Captor
    private ArgumentCaptor<DoksysBrevbestilling> doksysBrevbestillingArgumentCaptor;

    @Mock
    private BehandlingService behandlingService;
    @Mock
    private ProsessinstansService prosessinstansService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    private final Behandling behandling = lagBehandling();

    private SendVedtaksbrevInnland sendVedtaksbrevInnland;

    @BeforeEach
    public void setUp() {
        when(behandlingService.hentBehandlingMedSaksopplysninger(BEHANDLINGID)).thenReturn(behandling);

        sendVedtaksbrevInnland = new SendVedtaksbrevInnland(behandlingService, behandlingsresultatService, prosessinstansService);
    }

    @Test
    void utfør_medFlereLovvalgsperioder_girUnntak() {
        Lovvalgsperiode lovvalgsperiode1 = lagLovvalgsperiodeArt16_1();
        Lovvalgsperiode lovvalgsperiode2 = lagLovvalgsperiode(FO_883_2004_ART12_1, LocalDate.now().plusDays(30), Landkoder.DK, false);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID))
            .thenReturn(lagBehandlingsresultat(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND, Set.of(lovvalgsperiode1, lovvalgsperiode2), null));
        var prosessinstans = lagProsessinstans();


        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> sendVedtaksbrevInnland.utfør(prosessinstans))
            .withMessageContaining("Flere enn en lovvalgsperiode er ikke støttet");
    }

    @Test
    void utfør_avslagManglendeOppl_bestillerAvslagManglendeOppl() {
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID))
            .thenReturn(lagBehandlingsresultatUtenPerioder(Behandlingsresultattyper.AVSLAG_MANGLENDE_OPPL));


        sendVedtaksbrevInnland.utfør(lagProsessinstans());


        verify(prosessinstansService).opprettProsessinstanserSendBrev(eq(behandling), doksysBrevbestillingArgumentCaptor.capture(), eq(List.of(Mottaker.av(BRUKER))));
        assertThat(doksysBrevbestillingArgumentCaptor.getValue().getProduserbartdokument()).isEqualTo(AVSLAG_MANGLENDE_OPPLYSNINGER);
        verify(prosessinstansService, never()).opprettProsessinstansSendBrev(eq(behandling), any(DoksysBrevbestilling.class), eq(Mottaker.av(ARBEIDSGIVER)));
    }

    @Test
    void utfør_utenPeriode_feiler() {
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID))
            .thenReturn(lagBehandlingsresultatUtenPerioder(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND));
        var prosessinstans = lagProsessinstans();


        assertThatExceptionOfType(NoSuchElementException.class)
            .isThrownBy(() -> sendVedtaksbrevInnland.utfør(prosessinstans))
            .withMessageContaining("Ingen lovvalgsperiode finnes");
    }

    @Test
    void utfør_innvilgelse12_1_vedtakOgKopiTilSkattSendes() {
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID))
            .thenReturn(lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(FO_883_2004_ART12_1)));


        sendVedtaksbrevInnland.utfør(lagProsessinstans());


        var mottakere = List.of(Mottaker.av(BRUKER), FastMottakerMedOrgnr.av(SKATT));
        verify(prosessinstansService).opprettProsessinstanserSendBrev(eq(behandling), doksysBrevbestillingArgumentCaptor.capture(), eq(mottakere));
        assertThat(doksysBrevbestillingArgumentCaptor.getValue().getProduserbartdokument()).isEqualTo(INNVILGELSE_YRKESAKTIV);
    }

    @Test
    void utfør_innvilgelse11_4_senderIkkeOrienteringTilArbeidsgiver() {
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID))
            .thenReturn(lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(FO_883_2004_ART11_4_2)));


        sendVedtaksbrevInnland.utfør(lagProsessinstans());


        var mottakere = List.of(Mottaker.av(BRUKER), FastMottakerMedOrgnr.av(SKATT));
        verify(prosessinstansService).opprettProsessinstanserSendBrev(eq(behandling), doksysBrevbestillingArgumentCaptor.capture(), eq(mottakere));
        assertThat(doksysBrevbestillingArgumentCaptor.getValue().getProduserbartdokument()).isEqualTo(INNVILGELSE_YRKESAKTIV);
        verify(prosessinstansService, never()).opprettProsessinstansSendBrev(eq(behandling), any(DoksysBrevbestilling.class), eq(Mottaker.av(ARBEIDSGIVER)));
    }

    @Test
    void utfør_innvilgelse13_1A_vedtakOgKopiTilSkattSendes() {
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID))
            .thenReturn(lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(FO_883_2004_ART13_1A)));


        sendVedtaksbrevInnland.utfør(lagProsessinstans());


        var mottakere = List.of(Mottaker.av(BRUKER), FastMottakerMedOrgnr.av(SKATT));
        verify(prosessinstansService).opprettProsessinstanserSendBrev(eq(behandling), doksysBrevbestillingArgumentCaptor.capture(), eq(mottakere));
        assertThat(doksysBrevbestillingArgumentCaptor.getValue().getProduserbartdokument()).isEqualTo(INNVILGELSE_YRKESAKTIV_FLERE_LAND);
    }

    @Test
    void utfør_utpeking13_1B1_senderOrienteringsbrev() {
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID))
            .thenReturn(lagBehandlingsresultat(lagUtpekingsperiode(), lagLovvalgsperiode(FO_883_2004_ART13_1B1, LocalDate.now(), Landkoder.SE, true)));


        sendVedtaksbrevInnland.utfør(lagProsessinstans());


        verify(prosessinstansService).opprettProsessinstansSendBrev(eq(behandling), doksysBrevbestillingArgumentCaptor.capture(), eq(Mottaker.av(BRUKER)));
        assertThat(doksysBrevbestillingArgumentCaptor.getValue().getProduserbartdokument()).isEqualTo(ORIENTERING_UTPEKING_UTLAND);
    }

    @Test
    void utfør_innvilgelse13_1A_senderIkkeInnvilgelseTilArbeidsgiver() {
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID))
            .thenReturn(lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(FO_883_2004_ART13_1A)));
        var prosessinstans = lagProsessinstans();


        sendVedtaksbrevInnland.utfør(prosessinstans);


        verify(prosessinstansService, never()).opprettProsessinstansSendBrev(eq(behandling), any(DoksysBrevbestilling.class), eq(Mottaker.av(ARBEIDSGIVER)));
    }

    @Test
    void utfør_innvilgelse13_1AMedUtenlandskForetak_senderBrevTilStatligSkatteoppkreving() {
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID))
            .thenReturn(lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(FO_883_2004_ART13_1A)));
        ForetakUtland arbeidsgiverUtland = new ForetakUtland();
        arbeidsgiverUtland.selvstendigNæringsvirksomhet = false;
        behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().foretakUtland.add(arbeidsgiverUtland);


        sendVedtaksbrevInnland.utfør(lagProsessinstans());


        var mottakere = List.of(Mottaker.av(BRUKER), FastMottakerMedOrgnr.av(SKATT), FastMottakerMedOrgnr.av(STATLIG_SKATTEOPPKREVING));
        verify(prosessinstansService).opprettProsessinstanserSendBrev(eq(behandling), doksysBrevbestillingArgumentCaptor.capture(), eq(mottakere));
        assertThat(doksysBrevbestillingArgumentCaptor.getValue().getProduserbartdokument()).isEqualTo(INNVILGELSE_YRKESAKTIV_FLERE_LAND);
    }

    @Test
    void utfør_innvilgelse16_1MedUtenlandskSelvstendigArbeid_senderIkkeBrevTilStatligSkatteoppkreving() {
        var behandlingsresultat = lagBehandlingsresultat(lagLovvalgsperiodeArt16_1());
        behandlingsresultat.getAnmodningsperioder().add(lagAnmodningsperiodeMedSvar());
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID)).thenReturn(behandlingsresultat);
        ForetakUtland utenlandskSelvstendigVirksomhet = new ForetakUtland();
        utenlandskSelvstendigVirksomhet.selvstendigNæringsvirksomhet = true;
        behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().foretakUtland.add(utenlandskSelvstendigVirksomhet);


        sendVedtaksbrevInnland.utfør(lagProsessinstans());


        var mottakere = List.of(Mottaker.av(BRUKER), FastMottakerMedOrgnr.av(SKATT));
        verify(prosessinstansService).opprettProsessinstanserSendBrev(eq(behandling), any(DoksysBrevbestilling.class), eq(mottakere));
    }

    @Test
    void utfør_innvilgelse16_1SaksbehandlerIkkeSatt_brukerSaksbehandlerSomAnmodetOmUnntakVedBrevbestilling() {
        var behandlingsresultat = lagBehandlingsresultat(lagLovvalgsperiodeArt16_1());
        behandlingsresultat.getAnmodningsperioder().add(lagAnmodningsperiodeMedSvar());
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID)).thenReturn(behandlingsresultat);
        var prosessinstans = lagProsessinstans();


        sendVedtaksbrevInnland.utfør(prosessinstans);


        assertThat(prosessinstans.getData(ProsessDataKey.SAKSBEHANDLER)).isNull();
        var mottakere = List.of(Mottaker.av(BRUKER), FastMottakerMedOrgnr.av(SKATT));
        verify(prosessinstansService).opprettProsessinstanserSendBrev(eq(behandling), doksysBrevbestillingArgumentCaptor.capture(), eq(mottakere));
        assertThat(doksysBrevbestillingArgumentCaptor.getValue().getAvsenderID()).isEqualTo("Z111111");
    }

    @Test
    void utfør_innvilgelse12_1_senderIkkeBrevTilStatligSkatteoppkreving() {
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID))
            .thenReturn(lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(FO_883_2004_ART12_1)));
        behandling.getBehandlingsgrunnlag().getBehandlingsgrunnlagdata().foretakUtland.add(new ForetakUtland());


        sendVedtaksbrevInnland.utfør(lagProsessinstans());


        var mottakere = List.of(Mottaker.av(BRUKER), FastMottakerMedOrgnr.av(SKATT));
        verify(prosessinstansService).opprettProsessinstanserSendBrev(eq(behandling), any(DoksysBrevbestilling.class), eq(mottakere));
    }

    @Test
    void utfør_innvilgelse12_1_senderInnvilgelseTilArbeidsgiver() {
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID))
            .thenReturn(lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(FO_883_2004_ART12_1)));


        sendVedtaksbrevInnland.utfør(lagProsessinstans());


        verify(prosessinstansService).opprettProsessinstansSendBrev(eq(behandling), doksysBrevbestillingArgumentCaptor.capture(), eq(Mottaker.av(ARBEIDSGIVER)));
        assertThat(doksysBrevbestillingArgumentCaptor.getValue().getProduserbartdokument()).isEqualTo(INNVILGELSE_ARBEIDSGIVER);
    }

    @Test
    void utfør_avslag12_1_senderTilHelfoOgSkatt() {
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID))
            .thenReturn(lagBehandlingsresultat(lagLovvalgsperiode(FO_883_2004_ART12_1, LocalDate.now(), Landkoder.HR, false)));


        sendVedtaksbrevInnland.utfør(lagProsessinstans());


        var mottakere = List.of(Mottaker.av(BRUKER), av(HELFO), av(SKATT));
        verify(prosessinstansService).opprettProsessinstanserSendBrev(eq(behandling), doksysBrevbestillingArgumentCaptor.capture(), eq(mottakere));
        assertThat(doksysBrevbestillingArgumentCaptor.getValue().getProduserbartdokument()).isEqualTo(AVSLAG_YRKESAKTIV);
    }

    @Test
    void utfør_avslag12_1MedArbeidsgiver_senderTilArbeidsgiver() {
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID))
            .thenReturn(lagBehandlingsresultat(lagLovvalgsperiode(FO_883_2004_ART12_1, LocalDate.now(), Landkoder.HR, false)));
        Aktoer arbeidsgiver = new Aktoer();
        arbeidsgiver.setRolle(ARBEIDSGIVER);
        arbeidsgiver.setOrgnr("123456789");
        behandling.getFagsak().getAktører().add(arbeidsgiver);


        sendVedtaksbrevInnland.utfør(lagProsessinstans());


        verify(prosessinstansService).opprettProsessinstansSendBrev(eq(behandling), doksysBrevbestillingArgumentCaptor.capture(), eq(Mottaker.av(ARBEIDSGIVER)));
        assertThat(doksysBrevbestillingArgumentCaptor.getValue().getProduserbartdokument()).isEqualTo(AVSLAG_ARBEIDSGIVER);
    }


    @Test
    void utfør_PåInnvilgelsesBrevBestemtAv12_2_senderBrevTilSkattOgKopiTilArbeidsgiver() {
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID))
            .thenReturn(lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(FO_883_2004_ART12_2)));


        sendVedtaksbrevInnland.utfør(lagProsessinstans());


        var mottakere = List.of(Mottaker.av(BRUKER), FastMottakerMedOrgnr.av(SKATT));
        verify(prosessinstansService).opprettProsessinstanserSendBrev(eq(behandling), any(DoksysBrevbestilling.class), eq(mottakere));
        verify(prosessinstansService).opprettProsessinstansSendBrev(eq(behandling), doksysBrevbestillingArgumentCaptor.capture(), eq(Mottaker.av(ARBEIDSGIVER)));
        assertThat(doksysBrevbestillingArgumentCaptor.getValue().getProduserbartdokument()).isEqualTo(INNVILGELSE_ARBEIDSGIVER);
    }

    @Test
    void utfør_PåFastsattLovvalgINorgeUtenInnvilgetBestemmelse_GårTilFeiletMaskinelt() {
        var behandlingsresultat = lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(FO_883_2004_ANNET));
        behandlingsresultat.setType(Behandlingsresultattyper.IKKE_FASTSATT);
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID)).thenReturn(behandlingsresultat);
        var prosessinstans = lagProsessinstans();


        assertThatExceptionOfType(FunksjonellException.class)
            .isThrownBy(() -> sendVedtaksbrevInnland.utfør(prosessinstans))
            .withMessageContaining("Vedtaksbrev kan ikke sendes for behandling");
    }

    @Test
    void utfør_PåInnvilgelsesBrev12_1_medBegrunnelsekodeForkortetPeriode_oppdatererBrevdata() {
        var behandlingsresultat = lagBehandlingsresultat(lagInnvilgetLovvalgsperiode(FO_883_2004_ART12_1));
        behandlingsresultat.getAvklartefakta().add(
            new Avklartefakta(
                behandlingsresultat,
                Avklartefaktatyper.AARSAK_ENDRING_PERIODE.getKode(),
                Avklartefaktatyper.AARSAK_ENDRING_PERIODE,
                null,
                Endretperiode.ENDRINGER_ARBEIDSSITUASJON.getKode()));
        when(behandlingsresultatService.hentBehandlingsresultat(BEHANDLINGID)).thenReturn(behandlingsresultat);


        sendVedtaksbrevInnland.utfør(lagProsessinstans());


        var mottakere = List.of(Mottaker.av(BRUKER), FastMottakerMedOrgnr.av(SKATT));
        verify(prosessinstansService).opprettProsessinstanserSendBrev(eq(behandling), doksysBrevbestillingArgumentCaptor.capture(), eq(mottakere));
        assertThat(doksysBrevbestillingArgumentCaptor.getValue().getBegrunnelseKode()).isEqualTo(Endretperiode.ENDRINGER_ARBEIDSSITUASJON.getKode());
    }

    private Prosessinstans lagProsessinstans() {
        var prosessinstans = new Prosessinstans();
        prosessinstans.setBehandling(behandling);
        prosessinstans.setType(ProsessType.IVERKSETT_VEDTAK_EOS);
        var brevdata = new BrevData();
        prosessinstans.setData(ProsessDataKey.BREVDATA, brevdata);
        return prosessinstans;
    }

    private static Behandling lagBehandling() {
        Behandling behandling = new Behandling();
        behandling.setId(BEHANDLINGID);
        behandling.setType(Behandlingstyper.SOEKNAD);
        behandling.setBehandlingsgrunnlag(new Behandlingsgrunnlag());
        behandling.getBehandlingsgrunnlag().setBehandlingsgrunnlagdata(new Soeknad());
        behandling.setFagsak(lagFagsak());
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
        myndighet.setRolle(TRYGDEMYNDIGHET);
        myndighet.setInstitusjonId("SE:sesese123");

        Aktoer arbeidsgiver = new Aktoer();
        arbeidsgiver.setRolle(ARBEIDSGIVER);
        arbeidsgiver.setOrgnr("123456789");

        fagsak.setAktører(new HashSet<>(Set.of(aktør, arbeidsgiver, myndighet)));
        return fagsak;
    }

    private static Anmodningsperiode lagAnmodningsperiodeMedSvar() {
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setAnmodetAv("Z111111");
        anmodningsperiode.setAnmodningsperiodeSvar(new AnmodningsperiodeSvar());
        anmodningsperiode.getAnmodningsperiodeSvar().setAnmodningsperiodeSvarType(Anmodningsperiodesvartyper.INNVILGELSE);
        return anmodningsperiode;
    }

    private static Lovvalgsperiode lagLovvalgsperiodeArt16_1() {
        return lagInnvilgetLovvalgsperiode(FO_883_2004_ART16_1);
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
        utpekingsperiode.setBestemmelse(FO_883_2004_ART13_1B1);
        return utpekingsperiode;
    }

    private static Behandlingsresultat lagBehandlingsresultat(Lovvalgsperiode periode) {
        return lagBehandlingsresultat(Behandlingsresultattyper.FASTSATT_LOVVALGSLAND,
            Collections.singleton(periode),
            Landkoder.NO
        );
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
}
