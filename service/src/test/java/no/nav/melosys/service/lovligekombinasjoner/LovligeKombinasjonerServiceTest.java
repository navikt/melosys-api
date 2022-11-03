package no.nav.melosys.service.lovligekombinasjoner;

import java.util.Set;

import no.nav.melosys.domain.Aktoer;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static no.nav.melosys.domain.kodeverk.Sakstemaer.*;
import static no.nav.melosys.domain.kodeverk.Sakstyper.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class LovligeKombinasjonerServiceTest {

    private LovligeKombinasjonerService lovligeKombinasjonerService;

    @BeforeEach
    void setup() {
        lovligeKombinasjonerService = new LovligeKombinasjonerService();
    }

    @Test
    void hentMuligeTyper_temaForespørselTrygdemyndighet_returnererHenvendelse() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(EU_EOS, UNNTAK);
        Behandling behandling = behandlingMedTemaOgType(FORESPØRSEL_TRYGDEMYNDIGHET, HENVENDELSE);
        behandling.setFagsak(fagsak);
        var muligeTyper = lovligeKombinasjonerService.behandlinstyperSomKanEndresTil(behandling);
        assertThat(muligeTyper).containsExactly(HENVENDELSE);
    }

    @Test
    void hentMuligeTyper_temaArbeidThenestepersonEllerFly_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(EU_EOS, MEDLEMSKAP_LOVVALG);
        Behandling behandling = behandlingMedTemaOgType(ARBEID_TJENESTEPERSON_ELLER_FLY, NY_VURDERING);
        behandling.setFagsak(fagsak);
        var muligeTyper = lovligeKombinasjonerService.behandlinstyperSomKanEndresTil(behandling);
        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeTyper_EU_EOS_MEDLEMSKAP_LOVVALG_temaIkkeYrkesAktiv_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(EU_EOS, MEDLEMSKAP_LOVVALG);
        Behandling behandling = behandlingMedTemaOgType(IKKE_YRKESAKTIV, NY_VURDERING);
        behandling.setFagsak(fagsak);
        var muligeTyper = lovligeKombinasjonerService.behandlinstyperSomKanEndresTil(behandling);
        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeTyper_EU_EOS_UNNTAK_temaForespørselTrygdemyndighet_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(EU_EOS, UNNTAK);
        Behandling behandling = behandlingMedTemaOgType(FORESPØRSEL_TRYGDEMYNDIGHET, HENVENDELSE);
        behandling.setFagsak(fagsak);
        var muligeTyper = lovligeKombinasjonerService.behandlinstyperSomKanEndresTil(behandling);
        assertThat(muligeTyper).contains(HENVENDELSE);
    }

    @Test
    void hentMuligeTyper_EU_EOS_TRYGDEAVGIFT_temaYrkesaktiv_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(EU_EOS, TRYGDEAVGIFT);
        Behandling behandling = behandlingMedTemaOgType(YRKESAKTIV, KLAGE);
        behandling.setFagsak(fagsak);
        var muligeTyper = lovligeKombinasjonerService.behandlinstyperSomKanEndresTil(behandling);
        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeTyper_FTRL_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(FTRL, MEDLEMSKAP_LOVVALG);
        Behandling behandling = behandlingMedTemaOgType(YRKESAKTIV, KLAGE);
        behandling.setFagsak(fagsak);
        var muligeTyper = lovligeKombinasjonerService.behandlinstyperSomKanEndresTil(behandling);
        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeTyper_FTRL_LOVVALG_MEDLEMSKAP_temaPensjonist_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(FTRL, MEDLEMSKAP_LOVVALG);
        Behandling behandling = behandlingMedTemaOgType(PENSJONIST, KLAGE);
        behandling.setFagsak(fagsak);
        var muligeTyper = lovligeKombinasjonerService.behandlinstyperSomKanEndresTil(behandling);
        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeTyper_FTRL_TRYGDEAVGIFT_temaPensjonist_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(FTRL, TRYGDEAVGIFT);
        Behandling behandling = behandlingMedTemaOgType(PENSJONIST, KLAGE);
        behandling.setFagsak(fagsak);
        var muligeTyper = lovligeKombinasjonerService.behandlinstyperSomKanEndresTil(behandling);
        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeTyper_FTRL_TRYGDEAVGIFT_temaYrkesaktiv_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(FTRL, TRYGDEAVGIFT);
        Behandling behandling = behandlingMedTemaOgType(YRKESAKTIV, KLAGE);
        behandling.setFagsak(fagsak);
        var muligeTyper = lovligeKombinasjonerService.behandlinstyperSomKanEndresTil(behandling);
        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeTyper_TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(TRYGDEAVTALE, MEDLEMSKAP_LOVVALG);
        Behandling behandling = behandlingMedTemaOgType(YRKESAKTIV, KLAGE);
        behandling.setFagsak(fagsak);
        var muligeTyper = lovligeKombinasjonerService.behandlinstyperSomKanEndresTil(behandling);
        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeTyper_TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_temaPensjonist_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(TRYGDEAVTALE, MEDLEMSKAP_LOVVALG);
        Behandling behandling = behandlingMedTemaOgType(PENSJONIST, KLAGE);
        behandling.setFagsak(fagsak);
        var muligeTyper = lovligeKombinasjonerService.behandlinstyperSomKanEndresTil(behandling);
        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeTyper_TRYGDEAVTALE_UNNTAK_temaForespørselTrygdemyndighet_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(TRYGDEAVTALE, UNNTAK);
        Behandling behandling = behandlingMedTemaOgType(FORESPØRSEL_TRYGDEMYNDIGHET, HENVENDELSE);
        behandling.setFagsak(fagsak);
        var muligeTyper = lovligeKombinasjonerService.behandlinstyperSomKanEndresTil(behandling);
        assertThat(muligeTyper).contains(HENVENDELSE);
    }

    @Test
    void hentMuligeTyper_TRYGDEAVTALE_TRYGDEAVGIFT_temaYrkesaktiv_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(TRYGDEAVTALE, TRYGDEAVGIFT);
        Behandling behandling = behandlingMedTemaOgType(YRKESAKTIV, KLAGE);
        behandling.setFagsak(fagsak);
        var muligeTyper = lovligeKombinasjonerService.behandlinstyperSomKanEndresTil(behandling);
        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeTyper_TRYGDEAVTALE_TRYGDEAVGIFT_temaPensjonist_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(TRYGDEAVTALE, TRYGDEAVGIFT);
        Behandling behandling = behandlingMedTemaOgType(PENSJONIST, KLAGE);
        behandling.setFagsak(fagsak);
        var muligeTyper = lovligeKombinasjonerService.behandlinstyperSomKanEndresTil(behandling);
        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
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

        Set<Behandlingstyper> muligeBehandlingstyper = lovligeKombinasjonerService.hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, MEDLEMSKAP_LOVVALG, UTSENDT_ARBEIDSTAKER, sisteBehandling);
        assertThat(muligeBehandlingstyper)
            .hasSize(3)
            .containsExactly(NY_VURDERING, KLAGE, HENVENDELSE);
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

    private Fagsak fagsakMedSakstypeOgSakstema(Sakstyper sakstype, Sakstemaer sakstema) {
        Fagsak fagsak = new Fagsak();
        fagsak.setType(sakstype);
        fagsak.setTema(sakstema);
        Aktoer a1 = new Aktoer();
        a1.setRolle(Aktoersroller.BRUKER);
        fagsak.setAktører(Sets.newLinkedHashSet(a1));
        return fagsak;
    }

    private Behandling behandlingMedTemaOgType(Behandlingstema tema, Behandlingstyper type) {
        var behandling = new Behandling();
        behandling.setTema(tema);
        behandling.setType(type);
        return behandling;
    }
}
