package no.nav.melosys.service.lovligekombinasjoner;

import java.util.List;
import java.util.Set;

import io.getunleash.FakeUnleash;
import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.Fagsak;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsaarsaktyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.featuretoggle.ToggleName;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LovligeKombinasjonerSaksbehandlingServiceTest {

    @Mock
    private FagsakService fagsakService;
    @Mock
    private BehandlingService behandlingService;
    @Mock
    private BehandlingsresultatService behandlingsresultatService;

    private final FakeUnleash unleash = new FakeUnleash();
    private LovligeKombinasjonerSaksbehandlingService lovligeKombinasjonerSaksbehandlingService;

    @BeforeEach
    void setup() {
        unleash.enableAll();
        lovligeKombinasjonerSaksbehandlingService = new LovligeKombinasjonerSaksbehandlingService(fagsakService, behandlingService, behandlingsresultatService, unleash);
    }

    @Test
    void hentMuligeSakstyper_saksnummerErNull_returnererAlleSakstyper() {
        var muligeSakstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeSakstyper(null);


        assertThat(muligeSakstyper).hasSize(3).containsExactlyInAnyOrder(EU_EOS, FTRL, TRYGDEAVTALE);
    }

    @Test
    void hentMuligeSakstyper_saksnummerIkkeNullSakKanEndres_returnererAlleSakstyper() {
        var fagsak = new Fagsak();
        fagsak.getBehandlinger().add(new Behandling());
        when(fagsakService.hentFagsak("saksnummer")).thenReturn(fagsak);


        var muligeSakstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeSakstyper("saksnummer");


        assertThat(muligeSakstyper).hasSize(3).containsExactlyInAnyOrder(EU_EOS, FTRL, TRYGDEAVTALE);
    }

    @Test
    void hentMuligeSakstyper_saksnummerIkkeNullSakKanIkkeEndres_returnererTomListe() {
        var fagsak = new Fagsak();
        fagsak.getBehandlinger().add(new Behandling());
        fagsak.getBehandlinger().add(new Behandling());
        fagsak.getBehandlinger().get(0).setStatus(AVSLUTTET);
        when(fagsakService.hentFagsak("saksnummer")).thenReturn(fagsak);


        var muligeSakstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeSakstyper("saksnummer");


        assertThat(muligeSakstyper).isEmpty();
    }


    @Test
    void hentMuligeSakstemaer_saksnummerErNull_returnererLovligeSakstemaer() {
        var muligeSakstemaer = lovligeKombinasjonerSaksbehandlingService.hentMuligeSakstemaer(Aktoersroller.VIRKSOMHET, TRYGDEAVTALE, null);


        assertThat(muligeSakstemaer).isNotEmpty();
    }

    @Test
    void hentMuligeSakstemaer_saksnummerErIkkeNullSakKanEndres_returnererLovligeSakstemaer() {
        var fagsak = new Fagsak();
        fagsak.getBehandlinger().add(new Behandling());
        when(fagsakService.hentFagsak("saksnummer")).thenReturn(fagsak);


        var muligeSakstemaer = lovligeKombinasjonerSaksbehandlingService.hentMuligeSakstemaer(Aktoersroller.VIRKSOMHET, TRYGDEAVTALE, "saksnummer");


        assertThat(muligeSakstemaer).isNotEmpty();
    }

    @Test
    void hentMuligeSakstemaer_saksnummerErIkkeNullSakKanIkkeEndres_returnererTomListe() {
        var fagsak = new Fagsak();
        fagsak.getBehandlinger().add(new Behandling());
        fagsak.getBehandlinger().add(new Behandling());
        fagsak.getBehandlinger().get(0).setStatus(AVSLUTTET);
        when(fagsakService.hentFagsak("saksnummer")).thenReturn(fagsak);


        var muligeSakstemaer = lovligeKombinasjonerSaksbehandlingService.hentMuligeSakstemaer(Aktoersroller.VIRKSOMHET, TRYGDEAVTALE, "saksnummer");


        assertThat(muligeSakstemaer).isEmpty();
    }

    @Test
    void hentMuligeBehandlingstyper_temaArbeidThenestepersonEllerFly_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, MEDLEMSKAP_LOVVALG, ARBEID_TJENESTEPERSON_ELLER_FLY, null, null);


        assertThat(muligeTyper).containsExactlyInAnyOrder(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeBehandlingstyper_EU_EOS_MEDLEMSKAP_LOVVALG_temaIkkeYrkesAktiv_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, MEDLEMSKAP_LOVVALG, IKKE_YRKESAKTIV, null, null);


        assertThat(muligeTyper).containsExactlyInAnyOrder(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeBehandlingstyper_EU_EOS_UNNTAK_temaForespørselTrygdemyndighet_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, UNNTAK, FORESPØRSEL_TRYGDEMYNDIGHET, null, null);


        assertThat(muligeTyper).containsExactlyInAnyOrder(HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_EU_EOS_TRYGDEAVGIFT_temaYrkesaktiv_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, TRYGDEAVGIFT, YRKESAKTIV, null, null);


        assertThat(muligeTyper).containsExactlyInAnyOrder(ÅRSAVREGNING, NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeBehandlingstyper_FTRL_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_ikkeIKnyttTilSakKontekst_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, FTRL, MEDLEMSKAP_LOVVALG, YRKESAKTIV, null, null);


        assertThat(muligeTyper).containsExactlyInAnyOrder(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeBehandlingstyper_FTRL_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_iKnyttTilSakKontekstOgSakHarBehandlingMedManglendeInnbetaling_returnererLovligKombinasjon() {
        var sisteBehandling = sisteBehandling(MANGLENDE_INNBETALING_TRYGDEAVGIFT);
        when(behandlingService.hentBehandling(sisteBehandling.getId())).thenReturn(sisteBehandling);


        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, FTRL, MEDLEMSKAP_LOVVALG, YRKESAKTIV, null, sisteBehandling.getId());


        assertThat(muligeTyper).containsExactlyInAnyOrder(NY_VURDERING, HENVENDELSE, KLAGE, MANGLENDE_INNBETALING_TRYGDEAVGIFT);
    }

    @Test
    void hentMuligeBehandlingstyper_FTRL_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_iKnyttTilSakKontekstMenSakHarIkkeBehandlingMedManglendeInnbetaling_returnererLovligKombinasjon() {
        var sisteBehandling = sisteBehandling(FØRSTEGANG);
        when(behandlingService.hentBehandling(sisteBehandling.getId())).thenReturn(sisteBehandling);


        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, FTRL, MEDLEMSKAP_LOVVALG, YRKESAKTIV, null, sisteBehandling.getId());


        assertThat(muligeTyper).containsExactlyInAnyOrder(NY_VURDERING, HENVENDELSE, KLAGE);
    }

    private Behandling sisteBehandling(Behandlingstyper behandlingstype) {
        var behandling = behandlingMedTemaOgType(YRKESAKTIV, behandlingstype);
        behandling.setId(1L);
        behandling.setStatus(AVSLUTTET);
        var fagsak = new Fagsak();
        fagsak.getBehandlinger().add(behandling);
        behandling.setFagsak(fagsak);
        return behandling;
    }

    @Test
    void hentMuligeBehandlingstyper_FTRL_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_toggleAv_returnererLovligKombinasjon() {
        unleash.enableAllExcept(ToggleName.SAKSBEHANDLING_MANGLENDE_INNBETALING);
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(Aktoersroller.BRUKER, FTRL, MEDLEMSKAP_LOVVALG, YRKESAKTIV, null, null);


        assertThat(muligeTyper).containsExactlyInAnyOrder(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeBehandlingstyper_FTRL_LOVVALG_MEDLEMSKAP_temaPensjonist_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, FTRL, MEDLEMSKAP_LOVVALG, PENSJONIST, null, null);

        assertThat(muligeTyper).containsExactlyInAnyOrder(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeBehandlingstyper_FTRL_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_toggleDisabled_returnererLovligKombinasjon() {
        unleash.enableAllExcept(ToggleName.SAKSBEHANDLING_MANGLENDE_INNBETALING);
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, FTRL, MEDLEMSKAP_LOVVALG, YRKESAKTIV, null, null);


        assertThat(muligeTyper).containsExactlyInAnyOrder(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeBehandlingstyper_FTRL_LOVVALG_MEDLEMSKAP_temaPensjonist_toggleDisabled_returnererLovligKombinasjon() {
        unleash.enableAllExcept(ToggleName.SAKSBEHANDLING_MANGLENDE_INNBETALING);
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, FTRL, MEDLEMSKAP_LOVVALG, PENSJONIST, null, null);


        assertThat(muligeTyper).containsExactlyInAnyOrder(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeBehandlingstyper_FTRL_TRYGDEAVGIFT_temaPensjonist_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, FTRL, TRYGDEAVGIFT, PENSJONIST, null, null);


        assertThat(muligeTyper).containsExactlyInAnyOrder(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeBehandlingstyper_FTRL_TRYGDEAVGIFT_temaYrkesaktiv_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, FTRL, TRYGDEAVGIFT, YRKESAKTIV, null, null);


        assertThat(muligeTyper).containsExactlyInAnyOrder(ÅRSAVREGNING, NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeBehandlingstyper_TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, TRYGDEAVTALE, MEDLEMSKAP_LOVVALG, YRKESAKTIV, null, null);


        assertThat(muligeTyper).containsExactlyInAnyOrder(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeBehandlingstyper_TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_temaPensjonist_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, TRYGDEAVTALE, MEDLEMSKAP_LOVVALG, PENSJONIST, null, null);


        assertThat(muligeTyper).containsExactlyInAnyOrder(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeBehandlingstyper_TRYGDEAVTALE_UNNTAK_temaForespørselTrygdemyndighet_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, TRYGDEAVTALE, UNNTAK, FORESPØRSEL_TRYGDEMYNDIGHET, null, null);


        assertThat(muligeTyper).containsExactlyInAnyOrder(HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_TRYGDEAVTALE_TRYGDEAVGIFT_temaYrkesaktiv_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, TRYGDEAVTALE, TRYGDEAVGIFT, YRKESAKTIV, null, null);


        assertThat(muligeTyper).containsExactlyInAnyOrder(ÅRSAVREGNING, NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeBehandlingstyper_TRYGDEAVTALE_TRYGDEAVGIFT_temaPensjonist_returnererLovligKombinasjon() {
        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, TRYGDEAVTALE, TRYGDEAVGIFT, PENSJONIST, null, null);


        assertThat(muligeTyper).containsExactlyInAnyOrder(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeBehandlingstyper_avsluttet_returnererIkkeFørstegang() {
        Behandling sisteBehandling = behandlingMedTemaOgType(UTSENDT_ARBEIDSTAKER, FØRSTEGANG);
        sisteBehandling.setStatus(AVSLUTTET);
        sisteBehandling.setFagsak(new Fagsak());

        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, MEDLEMSKAP_LOVVALG, UTSENDT_ARBEIDSTAKER, null, sisteBehandling, null);


        assertThat(muligeTyper).containsExactlyInAnyOrder(NY_VURDERING, HENVENDELSE, KLAGE).doesNotContain(FØRSTEGANG);
    }

    @Test
    void hentMuligeBehandlingstyper_midlertidigLovvalgsbesluttet_returnererIkkeFørstegang() {
        Behandling sisteBehandling = behandlingMedTemaOgType(UTSENDT_ARBEIDSTAKER, FØRSTEGANG);
        sisteBehandling.setStatus(MIDLERTIDIG_LOVVALGSBESLUTNING);
        sisteBehandling.setFagsak(new Fagsak());

        var muligeTyper = lovligeKombinasjonerSaksbehandlingService
            .hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, MEDLEMSKAP_LOVVALG, UTSENDT_ARBEIDSTAKER, null, sisteBehandling, null);


        assertThat(muligeTyper).containsExactlyInAnyOrder(NY_VURDERING, HENVENDELSE, KLAGE).doesNotContain(FØRSTEGANG);
    }

    @Test
    void hentMuligeBehandlingstemaer_hovedpartVIRKSOMHETIkkeTrygdeavgift_skalReturnereBehandlingsTemaVIRKSOMHET() {
        Set<Behandlingstema> behandlingstemas = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(Aktoersroller.VIRKSOMHET, TRYGDEAVTALE, MEDLEMSKAP_LOVVALG, null, null);
        assertThat(behandlingstemas)
            .hasSize(1)
            .containsExactlyInAnyOrder(VIRKSOMHET);
    }

    @Test
    void hentMuligeBehandlingstemaer_hovedpartVIRKSOMHETTrygdeavgift_skalReturnereBehandlingsTemaYRKESAKTIV() {
        Set<Behandlingstema> behandlingstemas = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(Aktoersroller.VIRKSOMHET, TRYGDEAVTALE, TRYGDEAVGIFT, null, null);
        assertThat(behandlingstemas)
            .hasSize(1)
            .containsExactlyInAnyOrder(YRKESAKTIV);
    }

    @Test
    void hentMuligeBehandlingstemaer_ingenHovedpart_skalReturnereSammeSomHovedpartVIRKSOMHETogBRUKER() {
        Set<Behandlingstema> behandlingstemaer = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(null, TRYGDEAVTALE, TRYGDEAVGIFT, null, null);

        Set<Behandlingstema> behandlingstemaerVirksomhet = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(Aktoersroller.VIRKSOMHET, TRYGDEAVTALE, TRYGDEAVGIFT, null, null);
        Set<Behandlingstema> behandlingstemaerBruker = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(Aktoersroller.BRUKER, TRYGDEAVTALE, TRYGDEAVGIFT, null, null);

        assertThat(behandlingstemaer)
            .containsAll(behandlingstemaerVirksomhet)
            .containsAll(behandlingstemaerBruker);
    }

    @Test
    void hentMuligeBehandlingstemaer_ingenHovedpartMedlemskapLovvalg_skalReturnereSedBehandlingstema() {
        Set<Behandlingstema> behandlingstemaer = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(null, EU_EOS, MEDLEMSKAP_LOVVALG, null, null);

        assertThat(behandlingstemaer)
            .contains(BESLUTNING_LOVVALG_NORGE);
    }

    @Test
    void hentMuligeBehandlingstemaer_ingenHovedpartUnntak_skalReturnereSedBehandlingstema() {
        Set<Behandlingstema> behandlingstemaer = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(null, EU_EOS, UNNTAK, null, null);

        assertThat(behandlingstemaer)
            .contains(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE, BESLUTNING_LOVVALG_ANNET_LAND, ANMODNING_OM_UNNTAK_HOVEDREGEL);
    }

    @Test
    void hentMuligeBehandlingstemaer_ingenHovedpartTrygdeavgift_skalReturnereIngenSedBehandlingstema() {
        Set<Behandlingstema> behandlingstemaer = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(null, EU_EOS, TRYGDEAVGIFT, null, null);

        assertThat(behandlingstemaer)
            .isNotEmpty()
            .doesNotContain(BESLUTNING_LOVVALG_NORGE, REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE, BESLUTNING_LOVVALG_ANNET_LAND, ANMODNING_OM_UNNTAK_HOVEDREGEL);
    }

    @Test
    void hentMuligeBehandlingstemaer_EuEosUnntak_skalReturnereA1AnmodningOmUnntakPapir() {
        Set<Behandlingstema> behandlingstemaer = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(Aktoersroller.BRUKER, EU_EOS, UNNTAK, null, null);

        assertThat(behandlingstemaer)
            .isNotEmpty()
            .containsExactlyInAnyOrder(A1_ANMODNING_OM_UNNTAK_PAPIR, FORESPØRSEL_TRYGDEMYNDIGHET);
    }

    @Test
    void hentMuligeBehandlingstemaer_manglendeInnbetalingTrygdeavgift_skalKunReturnereEksisterendeBehTema() {
        Behandling aktivBehandling = new Behandling();
        aktivBehandling.setTema(YRKESAKTIV);
        aktivBehandling.setType(MANGLENDE_INNBETALING_TRYGDEAVGIFT);
        when(behandlingService.hentBehandling(1L)).thenReturn(aktivBehandling);

        Set<Behandlingstema> behandlingstemaer = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(Aktoersroller.BRUKER, FTRL, MEDLEMSKAP_LOVVALG, 1L, null);

        assertThat(behandlingstemaer)
            .isNotEmpty()
            .hasSize(1)
            .containsExactly(YRKESAKTIV);
    }

    @Test
    void hentMuligeBehandlingstemaer_sistBehandlingstemaErSedTema_skalKunReturnereSistBehandlingstema() {
        Set<Behandlingstema> behandlingstemaer = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstemaer(Aktoersroller.BRUKER, EU_EOS, UNNTAK, null, ANMODNING_OM_UNNTAK_HOVEDREGEL);

        assertThat(behandlingstemaer)
            .isNotEmpty()
            .hasSize(1)
            .containsExactly(ANMODNING_OM_UNNTAK_HOVEDREGEL);
    }

    @Test
    void hentMuligeBehandlingstyper_sisteBehandlingFinnes_skalIkkeReturnereFørstegangsbehandling() {
        Fagsak fagsak = new Fagsak();
        fagsak.setType(EU_EOS);
        Behandling sisteBehandling = behandlingMedTemaOgType(UTSENDT_ARBEIDSTAKER, FØRSTEGANG);
        sisteBehandling.setFagsak(fagsak);
        sisteBehandling.setStatus(Behandlingsstatus.AVSLUTTET);

        Set<Behandlingstyper> muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, MEDLEMSKAP_LOVVALG, UTSENDT_ARBEIDSTAKER, null, sisteBehandling, null);
        assertThat(muligeBehandlingstyper)
            .hasSize(3)
            .containsExactly(NY_VURDERING, KLAGE, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_unntakA1AnmodningOmUnntakPåPapir_skalReturnereBehTypeFØRSTEGANG_NY_VURDERING_KLAGE() {
        Set<Behandlingstyper> muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, UNNTAK, A1_ANMODNING_OM_UNNTAK_PAPIR, null, null, null);
        assertThat(muligeBehandlingstyper)
            .hasSize(4)
            .containsExactly(FØRSTEGANG, NY_VURDERING, KLAGE, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_sisteBehandlingAktiv_skalReturnereTomListe() {
        Fagsak fagsak = new Fagsak();
        fagsak.setType(EU_EOS);
        Behandling sisteBehandling = behandlingMedTemaOgType(UTSENDT_ARBEIDSTAKER, FØRSTEGANG);
        sisteBehandling.setFagsak(fagsak);
        sisteBehandling.setStatus(UNDER_BEHANDLING);


        Set<Behandlingstyper> muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, MEDLEMSKAP_LOVVALG, UTSENDT_ARBEIDSTAKER, null, sisteBehandling, new Behandlingsresultat());


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


        Set<Behandlingstyper> muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, MEDLEMSKAP_LOVVALG, UTSENDT_ARBEIDSTAKER, null, sisteBehandling, sisteBehandlingsresultat);


        assertThat(muligeBehandlingstyper)
            .hasSize(1)
            .containsExactly(NY_VURDERING);
    }

    @Test
    void hentMuligeBehandlingstyper_aktivBehandlingSomErFørst_skalReturnereAlleBehandlingstyper() {
        Fagsak fagsak = new Fagsak();
        fagsak.setType(EU_EOS);
        Behandling aktivBehandling = behandlingMedTemaOgType(UTSENDT_ARBEIDSTAKER, FØRSTEGANG);
        aktivBehandling.setId(1L);
        aktivBehandling.setFagsak(fagsak);
        aktivBehandling.setStatus(UNDER_BEHANDLING);
        fagsak.getBehandlinger().add(aktivBehandling);
        when(behandlingService.hentBehandling(aktivBehandling.getId())).thenReturn(aktivBehandling);


        Set<Behandlingstyper> muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, MEDLEMSKAP_LOVVALG, UTSENDT_ARBEIDSTAKER, aktivBehandling.getId(), null);


        assertThat(muligeBehandlingstyper).containsExactly(FØRSTEGANG, NY_VURDERING, KLAGE, HENVENDELSE);
    }

    @Test
    void hentMuligeBehandlingstyper_aktivBehandlingSomIkkeErFørst_skalIkkeHaFørstegang() {
        Fagsak fagsak = new Fagsak();
        fagsak.setType(EU_EOS);

        Behandling forrigeBehandling = behandlingMedTemaOgType(UTSENDT_ARBEIDSTAKER, FØRSTEGANG);
        forrigeBehandling.setId(1L);
        forrigeBehandling.setFagsak(fagsak);
        forrigeBehandling.setStatus(AVSLUTTET);

        Behandling aktivBehandling = behandlingMedTemaOgType(UTSENDT_ARBEIDSTAKER, NY_VURDERING);
        aktivBehandling.setId(2L);
        aktivBehandling.setFagsak(fagsak);
        aktivBehandling.setStatus(UNDER_BEHANDLING);
        fagsak.getBehandlinger().addAll(List.of(forrigeBehandling, aktivBehandling));

        when(behandlingService.hentBehandling(aktivBehandling.getId())).thenReturn(aktivBehandling);


        Set<Behandlingstyper> muligeBehandlingstyper = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingstyper(Aktoersroller.BRUKER, EU_EOS, MEDLEMSKAP_LOVVALG, UTSENDT_ARBEIDSTAKER, aktivBehandling.getId(), null);


        assertThat(muligeBehandlingstyper).isNotEmpty().doesNotContain(FØRSTEGANG);
    }

    @Test
    void hentMuligeBehandlingsårsaktyper_behandlingstypeErManglendeInnbetalingTrygdeavgift() {
        var typer = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingsårsaktyper(MANGLENDE_INNBETALING_TRYGDEAVGIFT);
        assertThat(typer).containsExactly(Behandlingsaarsaktyper.MELDING_OM_MANGLENDE_INNBETALING);
    }

    @Test
    void hentMuligeBehandlingsårsaktyper_behandlingstypeErAnnetEnnManglendeInnbetalingTrygdeavgift() {
        var typer = lovligeKombinasjonerSaksbehandlingService.hentMuligeBehandlingsårsaktyper(FØRSTEGANG);
        assertThat(typer).containsExactly(Behandlingsaarsaktyper.SØKNAD, Behandlingsaarsaktyper.SED, Behandlingsaarsaktyper.HENVENDELSE, Behandlingsaarsaktyper.FRITEKST);
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
