package no.nav.melosys.service.lovligekombinasjoner;

import java.util.List;
import java.util.Set;

import io.getunleash.FakeUnleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.service.behandling.BehandlingService;
import no.nav.melosys.service.behandling.BehandlingsresultatService;
import no.nav.melosys.service.sak.FagsakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.Sakstemaer.*;
import static no.nav.melosys.domain.kodeverk.Sakstyper.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LovligeKombinasjonerSaksbehandlingServiceUtenKlageTest {

    /*
     *      Tester for når melosys.behandlingstype.klage er disabled
     */

    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    private LovligeKombinasjonerSaksbehandlingService lovligeKombinasjonerSaksbehandlingService;

    @BeforeEach
    void setup() {
        lovligeKombinasjonerSaksbehandlingService = new LovligeKombinasjonerSaksbehandlingService(fagsakService, behandlingService, behandlingsresultatService, new FakeUnleash());
    }

    @Test
    void hentMuligeSakstyper_saksnummerErNull_returnererAlleSakstyper() {
        var muligeSakstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeSakstyper(null);


        assertThat(muligeSakstyper).hasSize(3).contains(EU_EOS, FTRL, TRYGDEAVTALE);
    }

    @Test
    void hentMuligeSakstyper_saksnummerIkkeNullSakKanEndres_returnererAlleSakstyper() {
        var fagsak = FagsakTestFactory.builder().behandlinger(new Behandling()).build();
        when(fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER)).thenReturn(fagsak);


        var muligeSakstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeSakstyper(FagsakTestFactory.SAKSNUMMER);


        assertThat(muligeSakstyper).hasSize(3).contains(EU_EOS, FTRL, TRYGDEAVTALE);
    }

    @Test
    void hentMuligeSakstyper_saksnummerIkkeNullSakKanIkkeEndres_returnererTomListe() {
        var behandling1 = new Behandling();
        behandling1.setStatus(AVSLUTTET);
        var behandling2 = new Behandling();
        var fagsak = FagsakTestFactory.builder().behandlinger(List.of(behandling1, behandling2)).build();
        when(fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER)).thenReturn(fagsak);


        var muligeSakstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeSakstyper(FagsakTestFactory.SAKSNUMMER);


        assertThat(muligeSakstyper).isEmpty();
    }


    @Test
    void hentMuligeSakstemaer_saksnummerErNull_returnererLovligeSakstemaer() {
        var muligeSakstemaer = lovligeKombinasjonerSaksbehandlingService.hentMuligeSakstemaer(Aktoersroller.VIRKSOMHET, TRYGDEAVTALE, null);


        assertThat(muligeSakstemaer).isNotEmpty();
    }

    @Test
    void hentMuligeSakstemaer_saksnummerErIkkeNullSakKanEndres_returnererLovligeSakstemaer() {
        var fagsak = FagsakTestFactory.builder().behandlinger(new Behandling()).build();
        when(fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER)).thenReturn(fagsak);


        var muligeSakstemaer = lovligeKombinasjonerSaksbehandlingService.hentMuligeSakstemaer(Aktoersroller.VIRKSOMHET, TRYGDEAVTALE, FagsakTestFactory.SAKSNUMMER);


        assertThat(muligeSakstemaer).isNotEmpty();
    }

    @Test
    void hentMuligeSakstemaer_saksnummerErIkkeNullSakKanIkkeEndres_returnererTomListe() {
        var behandling1 = new Behandling();
        behandling1.setStatus(AVSLUTTET);
        var behandling2 = new Behandling();
        var fagsak = FagsakTestFactory.builder().behandlinger(List.of(behandling1, behandling2)).build();
        when(fagsakService.hentFagsak(FagsakTestFactory.SAKSNUMMER)).thenReturn(fagsak);


        var muligeSakstemaer = lovligeKombinasjonerSaksbehandlingService.hentMuligeSakstemaer(Aktoersroller.VIRKSOMHET, TRYGDEAVTALE, FagsakTestFactory.SAKSNUMMER);


        assertThat(muligeSakstemaer).isEmpty();
    }

    @Test
    void hentMuligeBehandlingstyper_temaArbeidThenestepersonEllerFly_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, MEDLEMSKAP_LOVVALG, ARBEID_TJENESTEPERSON_ELLER_FLY, null, null);


        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_EU_EOS_MEDLEMSKAP_LOVVALG_temaIkkeYrkesAktiv_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, MEDLEMSKAP_LOVVALG, IKKE_YRKESAKTIV, null, null);


        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_EU_EOS_UNNTAK_temaForespørselTrygdemyndighet_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, UNNTAK, FORESPØRSEL_TRYGDEMYNDIGHET, null, null);


        assertThat(muligeTyper).contains(HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_EU_EOS_TRYGDEAVGIFT_temaYrkesaktiv_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, TRYGDEAVGIFT, YRKESAKTIV, null, null);


        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_FTRL_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, FTRL, MEDLEMSKAP_LOVVALG, YRKESAKTIV, null, null);


        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_FTRL_LOVVALG_MEDLEMSKAP_temaPensjonist_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, FTRL, MEDLEMSKAP_LOVVALG, PENSJONIST, null, null);


        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_FTRL_TRYGDEAVGIFT_temaPensjonist_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, FTRL, TRYGDEAVGIFT, PENSJONIST, null, null);


        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_FTRL_TRYGDEAVGIFT_temaYrkesaktiv_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, FTRL, TRYGDEAVGIFT, YRKESAKTIV, null, null);


        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, TRYGDEAVTALE, MEDLEMSKAP_LOVVALG, YRKESAKTIV, null, null);


        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_temaPensjonist_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, TRYGDEAVTALE, MEDLEMSKAP_LOVVALG, PENSJONIST, null, null);


        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_TRYGDEAVTALE_UNNTAK_temaForespørselTrygdemyndighet_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, TRYGDEAVTALE, UNNTAK, FORESPØRSEL_TRYGDEMYNDIGHET, null, null);


        assertThat(muligeTyper).contains(HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_TRYGDEAVTALE_TRYGDEAVGIFT_temaYrkesaktiv_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, TRYGDEAVTALE, TRYGDEAVGIFT, YRKESAKTIV, null, null);


        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_TRYGDEAVTALE_TRYGDEAVGIFT_temaPensjonist_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, TRYGDEAVTALE, TRYGDEAVGIFT, PENSJONIST, null, null);


        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstemaer_hovedpartVIRKSOMHETIkkeTrygdeavgift_skalReturnereBehandlingsTemaVIRKSOMHET() {
        Set<Behandlingstema> behandlingstemas = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(Aktoersroller.VIRKSOMHET, TRYGDEAVTALE, MEDLEMSKAP_LOVVALG, null, null);
        assertThat(behandlingstemas)
            .hasSize(1)
            .contains(VIRKSOMHET);
    }

    @Test
    void hentMuligeBehandlingstemaer_hovedpartVIRKSOMHETTrygdeavgift_skalReturnereBehandlingsTemaYRKESAKTIV() {
        Set<Behandlingstema> behandlingstemas = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(Aktoersroller.VIRKSOMHET, TRYGDEAVTALE, TRYGDEAVGIFT, null, null);
        assertThat(behandlingstemas)
            .hasSize(1)
            .contains(YRKESAKTIV);
    }

    @Test
    void hentMuligeBehandlingstemaer_ingenHovedpart_skalReturnereSammeSomHovedpartVIRKSOMHETgBRUKER() {
        Set<Behandlingstema> behandlingstemaer = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(null, TRYGDEAVTALE, TRYGDEAVGIFT, null, null);

        Set<Behandlingstema> behandlingstemaerVirksomhet = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(Aktoersroller.VIRKSOMHET, TRYGDEAVTALE, TRYGDEAVGIFT, null, null);
        Set<Behandlingstema> behandlingstemaerBruker = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(Aktoersroller.BRUKER, TRYGDEAVTALE, TRYGDEAVGIFT, null, null);

        assertThat(behandlingstemaer)
            .containsAll(behandlingstemaerVirksomhet)
            .containsAll(behandlingstemaerBruker);
    }

    @Test
    void hentMuligeBehandlingstemaer_ingenHovedpartUnntak_skalReturnereSedBehandlingstema() {
        Set<Behandlingstema> behandlingstemaer = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(null, EU_EOS, UNNTAK, null, null);

        assertThat(behandlingstemaer)
            .contains(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE, BESLUTNING_LOVVALG_ANNET_LAND);
    }

    @Test
    void hentMuligeBehandlingstyper_sisteBehandlingFinnes_skalIkkeReturnereFørstegangsbehandling() {
        Fagsak fagsak = FagsakTestFactory.lagFagsak();
        Behandling sisteBehandling = behandlingMedTemaOgType(UTSENDT_ARBEIDSTAKER, FØRSTEGANG);
        sisteBehandling.setFagsak(fagsak);
        sisteBehandling.setStatus(Behandlingsstatus.AVSLUTTET);

        Set<Behandlingstyper> muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, MEDLEMSKAP_LOVVALG, UTSENDT_ARBEIDSTAKER, null, sisteBehandling, null);
        assertThat(muligeBehandlingstyper)
            .hasSize(2)
            .containsExactly(NY_VURDERING, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_sisteBehandlingAktiv_skalReturnereTomListe() {
        Fagsak fagsak = FagsakTestFactory.lagFagsak();
        Behandling sisteBehandling = behandlingMedTemaOgType(UTSENDT_ARBEIDSTAKER, FØRSTEGANG);
        sisteBehandling.setFagsak(fagsak);
        sisteBehandling.setStatus(UNDER_BEHANDLING);


        Set<Behandlingstyper> muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, MEDLEMSKAP_LOVVALG, UTSENDT_ARBEIDSTAKER, null, sisteBehandling, new Behandlingsresultat());


        assertThat(muligeBehandlingstyper).isEmpty();
    }

    @Test
    void hentMuligeBehandlingstyper_sisteBehandlingAktivOgAnmodningsperiodeSendt_skalReturnereKunNyVurdering() {
        Fagsak fagsak = FagsakTestFactory.lagFagsak();
        Behandling sisteBehandling = behandlingMedTemaOgType(UTSENDT_ARBEIDSTAKER, FØRSTEGANG);
        sisteBehandling.setFagsak(fagsak);
        sisteBehandling.setStatus(UNDER_BEHANDLING);
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setSendtUtland(true);
        Behandlingsresultat sisteBehandlingsresultat = new Behandlingsresultat();
        sisteBehandlingsresultat.getAnmodningsperioder().add(anmodningsperiode);


        Set<Behandlingstyper> muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, MEDLEMSKAP_LOVVALG, UTSENDT_ARBEIDSTAKER, null, sisteBehandling, sisteBehandlingsresultat);


        assertThat(muligeBehandlingstyper)
            .hasSize(1)
            .containsExactly(NY_VURDERING);
    }

    @Test
    void hentMuligeBehandlingstyper_senderKunMedBehandlingID_henterBehandlingOgBehandlingsresultat() {
        lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, MEDLEMSKAP_LOVVALG, UTSENDT_ARBEIDSTAKER, null, 1L);


        verify(behandlingService).hentBehandling(1L);
        verify(behandlingsresultatService).hentBehandlingsresultatMedAnmodningsperioder(1L);
    }

    @Test
    void hentMuligeBehandlingStatuser_harNøyaktigRekkefølgePåMuligeStatus() {
        var muligeStatuser = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingStatuser();
        assertThat(muligeStatuser)
            .containsExactly(UNDER_BEHANDLING, AVVENT_DOK_PART, AVVENT_DOK_UTL, AVVENT_FAGLIG_AVKLARING);
    }

    @Test
    void hentMuligeBehandlingStatuser_avsluttetErIkkeMulig() {
        var muligeStatuser = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingStatuser();
        assertThat(muligeStatuser)
            .containsExactlyInAnyOrder(UNDER_BEHANDLING, AVVENT_DOK_PART, AVVENT_DOK_UTL, AVVENT_FAGLIG_AVKLARING)
            .doesNotContain(AVSLUTTET);
    }

    private Behandling behandlingMedTemaOgType(Behandlingstema tema, Behandlingstyper type) {
        var behandling = new Behandling();
        behandling.setTema(tema);
        behandling.setType(type);
        return behandling;
    }
}
