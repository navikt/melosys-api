package no.nav.melosys.service.lovligekombinasjoner;

import java.util.Set;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
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
class LovligeKombinasjonerServiceUtenKlageTest {

    /*
     *      Tester for når melosys.behandlingstype.klage er disabled
     */

    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    private LovligeKombinasjonerService lovligeKombinasjonerService;

    @BeforeEach
    void setup() {
        lovligeKombinasjonerService = new LovligeKombinasjonerService(fagsakService, behandlingService, behandlingsresultatService, new FakeUnleash());
    }

    @Test
    void hentMuligeSakstyper_saksnummerErNull_returnererAlleSakstyper() {
        var muligeSakstyper = lovligeKombinasjonerService.hentMuligeSakstyper(null);


        assertThat(muligeSakstyper).hasSize(3).contains(EU_EOS, FTRL, TRYGDEAVTALE);
    }

    @Test
    void hentMuligeSakstyper_saksnummerIkkeNullSakKanEndres_returnererAlleSakstyper() {
        var fagsak = new Fagsak();
        fagsak.getBehandlinger().add(new Behandling());
        when(fagsakService.hentFagsak("saksnummer")).thenReturn(fagsak);


        var muligeSakstyper = lovligeKombinasjonerService.hentMuligeSakstyper("saksnummer");


        assertThat(muligeSakstyper).hasSize(3).contains(EU_EOS, FTRL, TRYGDEAVTALE);
    }

    @Test
    void hentMuligeSakstyper_saksnummerIkkeNullSakKanIkkeEndres_returnererTomListe() {
        var fagsak = new Fagsak();
        fagsak.getBehandlinger().add(new Behandling());
        fagsak.getBehandlinger().add(new Behandling());
        fagsak.getBehandlinger().get(0).setStatus(AVSLUTTET);
        when(fagsakService.hentFagsak("saksnummer")).thenReturn(fagsak);


        var muligeSakstyper = lovligeKombinasjonerService.hentMuligeSakstyper("saksnummer");


        assertThat(muligeSakstyper).isEmpty();
    }


    @Test
    void hentMuligeSakstemaer_saksnummerErNull_returnererLovligeSakstemaer() {
        var muligeSakstemaer = lovligeKombinasjonerService.hentMuligeSakstemaer(Aktoersroller.VIRKSOMHET, TRYGDEAVTALE, null);


        assertThat(muligeSakstemaer).isNotEmpty();
    }

    @Test
    void hentMuligeSakstemaer_saksnummerErIkkeNullSakKanEndres_returnererLovligeSakstemaer() {
        var fagsak = new Fagsak();
        fagsak.getBehandlinger().add(new Behandling());
        when(fagsakService.hentFagsak("saksnummer")).thenReturn(fagsak);


        var muligeSakstemaer = lovligeKombinasjonerService.hentMuligeSakstemaer(Aktoersroller.VIRKSOMHET, TRYGDEAVTALE, "saksnummer");


        assertThat(muligeSakstemaer).isNotEmpty();
    }

    @Test
    void hentMuligeSakstemaer_saksnummerErIkkeNullSakKanIkkeEndres_returnererTomListe() {
        var fagsak = new Fagsak();
        fagsak.getBehandlinger().add(new Behandling());
        fagsak.getBehandlinger().add(new Behandling());
        fagsak.getBehandlinger().get(0).setStatus(AVSLUTTET);
        when(fagsakService.hentFagsak("saksnummer")).thenReturn(fagsak);


        var muligeSakstemaer = lovligeKombinasjonerService.hentMuligeSakstemaer(Aktoersroller.VIRKSOMHET, TRYGDEAVTALE, "saksnummer");


        assertThat(muligeSakstemaer).isEmpty();
    }

    @Test
    void hentMuligeBehandlingstyper_temaArbeidThenestepersonEllerFly_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, MEDLEMSKAP_LOVVALG, ARBEID_TJENESTEPERSON_ELLER_FLY, null, null);


        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_EU_EOS_MEDLEMSKAP_LOVVALG_temaIkkeYrkesAktiv_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, MEDLEMSKAP_LOVVALG, IKKE_YRKESAKTIV, null, null);


        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_EU_EOS_UNNTAK_temaForespørselTrygdemyndighet_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, UNNTAK, FORESPØRSEL_TRYGDEMYNDIGHET, null, null);


        assertThat(muligeTyper).contains(HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_EU_EOS_TRYGDEAVGIFT_temaYrkesaktiv_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, TRYGDEAVGIFT, YRKESAKTIV, null, null);


        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_FTRL_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, FTRL, MEDLEMSKAP_LOVVALG, YRKESAKTIV, null, null);


        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_FTRL_LOVVALG_MEDLEMSKAP_temaPensjonist_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, FTRL, MEDLEMSKAP_LOVVALG, PENSJONIST, null, null);


        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_FTRL_TRYGDEAVGIFT_temaPensjonist_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, FTRL, TRYGDEAVGIFT, PENSJONIST, null, null);


        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_FTRL_TRYGDEAVGIFT_temaYrkesaktiv_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, FTRL, TRYGDEAVGIFT, YRKESAKTIV, null, null);


        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, TRYGDEAVTALE, MEDLEMSKAP_LOVVALG, YRKESAKTIV, null, null);


        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_temaPensjonist_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, TRYGDEAVTALE, MEDLEMSKAP_LOVVALG, PENSJONIST, null, null);


        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_TRYGDEAVTALE_UNNTAK_temaForespørselTrygdemyndighet_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, TRYGDEAVTALE, UNNTAK, FORESPØRSEL_TRYGDEMYNDIGHET, null, null);


        assertThat(muligeTyper).contains(HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_TRYGDEAVTALE_TRYGDEAVGIFT_temaYrkesaktiv_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, TRYGDEAVTALE, TRYGDEAVGIFT, YRKESAKTIV, null, null);


        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_TRYGDEAVTALE_TRYGDEAVGIFT_temaPensjonist_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, TRYGDEAVTALE, TRYGDEAVGIFT, PENSJONIST, null, null);


        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstemaer_hovedpartVIRKSOMHETIkkeTrygdeavgift_skalReturnereBehandlingsTemaVIRKSOMHET() {
        Set<Behandlingstema> behandlingstemas = lovligeKombinasjonerService.hentMuligeBehandlingstemaer(Aktoersroller.VIRKSOMHET, TRYGDEAVTALE, MEDLEMSKAP_LOVVALG, null);
        assertThat(behandlingstemas)
            .hasSize(1)
            .contains(VIRKSOMHET);
    }

    @Test
    void hentMuligeBehandlingstemaer_hovedpartVIRKSOMHETTrygdeavgift_skalReturnereBehandlingsTemaYRKESAKTIV() {
        Set<Behandlingstema> behandlingstemas = lovligeKombinasjonerService.hentMuligeBehandlingstemaer(Aktoersroller.VIRKSOMHET, TRYGDEAVTALE, TRYGDEAVGIFT, null);
        assertThat(behandlingstemas)
            .hasSize(1)
            .contains(YRKESAKTIV);
    }

    @Test
    void hentMuligeBehandlingstemaer_ingenHovedpart_skalReturnereSammeSomHovedpartVIRKSOMHETgBRUKER() {
        Set<Behandlingstema> behandlingstemaer = lovligeKombinasjonerService.hentMuligeBehandlingstemaer(null, TRYGDEAVTALE, TRYGDEAVGIFT, null);

        Set<Behandlingstema> behandlingstemaerVirksomhet = lovligeKombinasjonerService.hentMuligeBehandlingstemaer(Aktoersroller.VIRKSOMHET, TRYGDEAVTALE, TRYGDEAVGIFT, null);
        Set<Behandlingstema> behandlingstemaerBruker = lovligeKombinasjonerService.hentMuligeBehandlingstemaer(Aktoersroller.BRUKER, TRYGDEAVTALE, TRYGDEAVGIFT, null);

        assertThat(behandlingstemaer)
            .containsAll(behandlingstemaerVirksomhet)
            .containsAll(behandlingstemaerBruker);
    }

    @Test
    void hentMuligeBehandlingstemaer_ingenHovedpartUnntak_skalReturnereSedBehandlingstema() {
        Set<Behandlingstema> behandlingstemaer = lovligeKombinasjonerService.hentMuligeBehandlingstemaer(null, EU_EOS, UNNTAK, null);

        assertThat(behandlingstemaer)
            .contains(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE, BESLUTNING_LOVVALG_ANNET_LAND);
    }

    @Test
    void hentMuligeBehandlingstyper_sisteBehandlingFinnes_skalIkkeReturnereFørstegangsbehandling() {
        Fagsak fagsak = new Fagsak();
        fagsak.setType(EU_EOS);
        Behandling sisteBehandling = behandlingMedTemaOgType(UTSENDT_ARBEIDSTAKER, FØRSTEGANG);
        sisteBehandling.setFagsak(fagsak);
        sisteBehandling.setStatus(Behandlingsstatus.AVSLUTTET);

        Set<Behandlingstyper> muligeBehandlingstyper = lovligeKombinasjonerService.hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, MEDLEMSKAP_LOVVALG, UTSENDT_ARBEIDSTAKER, null, sisteBehandling, null);
        assertThat(muligeBehandlingstyper)
            .hasSize(2)
            .containsExactly(NY_VURDERING, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_sisteBehandlingAktiv_skalReturnereTomListe() {
        Fagsak fagsak = new Fagsak();
        fagsak.setType(EU_EOS);
        Behandling sisteBehandling = behandlingMedTemaOgType(UTSENDT_ARBEIDSTAKER, FØRSTEGANG);
        sisteBehandling.setFagsak(fagsak);
        sisteBehandling.setStatus(UNDER_BEHANDLING);


        Set<Behandlingstyper> muligeBehandlingstyper = lovligeKombinasjonerService.hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, MEDLEMSKAP_LOVVALG, UTSENDT_ARBEIDSTAKER, null, sisteBehandling, null);


        assertThat(muligeBehandlingstyper).isEmpty();
    }

    @Test
    void hentMuligeBehandlingstyper_sisteBehandlingAktivOgAnmodningsperiodeSendt_skalReturnereKunNyVurdering() {
        Fagsak fagsak = new Fagsak();
        fagsak.setType(EU_EOS);
        Behandling sisteBehandling = behandlingMedTemaOgType(UTSENDT_ARBEIDSTAKER, FØRSTEGANG);
        sisteBehandling.setFagsak(fagsak);
        sisteBehandling.setStatus(UNDER_BEHANDLING);
        Anmodningsperiode anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setSendtUtland(true);
        Behandlingsresultat sisteBehandlingsresultat = new Behandlingsresultat();
        sisteBehandlingsresultat.getAnmodningsperioder().add(anmodningsperiode);


        Set<Behandlingstyper> muligeBehandlingstyper = lovligeKombinasjonerService.hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, MEDLEMSKAP_LOVVALG, UTSENDT_ARBEIDSTAKER, null, sisteBehandling, sisteBehandlingsresultat);


        assertThat(muligeBehandlingstyper)
            .hasSize(1)
            .containsExactly(NY_VURDERING);
    }

    @Test
    void hentMuligeBehandlingstyper_senderKunMedBehandlingID_henterBehandlingOgBehandlingsresultat() {
        lovligeKombinasjonerService.hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, MEDLEMSKAP_LOVVALG, UTSENDT_ARBEIDSTAKER, null, 1L);


        verify(behandlingService).hentBehandling(1L);
        verify(behandlingsresultatService).hentBehandlingsresultatMedAnmodningsperioder(1L);
    }

    @Test
    void hentMuligeBehandlingStatuser_harNøyaktigRekkefølgePåMuligeStatus() {
        var muligeStatuser = lovligeKombinasjonerService.hentMuligeBehandlingStatuser();
        assertThat(muligeStatuser)
            .containsExactly(UNDER_BEHANDLING, AVVENT_DOK_PART, AVVENT_DOK_UTL, AVVENT_FAGLIG_AVKLARING);
    }

    @Test
    void hentMuligeBehandlingStatuser_avsluttetErIkkeMulig() {
        var muligeStatuser = lovligeKombinasjonerService.hentMuligeBehandlingStatuser();
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
