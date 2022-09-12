package no.nav.melosys.service.behandling;

import java.util.Set;
import java.util.stream.Collectors;

import no.finn.unleash.FakeUnleash;
import no.nav.melosys.domain.*;
import no.nav.melosys.domain.kodeverk.Aktoersroller;
import no.nav.melosys.domain.kodeverk.Sakstemaer;
import no.nav.melosys.domain.kodeverk.Sakstyper;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import no.nav.melosys.service.lovligeKombinasjoner.LovligeSakKombinasjoner;
import org.assertj.core.util.Sets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.domain.Behandling.BEHANDLINGSTEMA_SED_FORESPØRSEL;
import static no.nav.melosys.domain.kodeverk.Sakstemaer.*;
import static no.nav.melosys.domain.kodeverk.Sakstyper.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*;
import static no.nav.melosys.service.behandling.MuligeManuelleBehandlingsendringer.BEHANDLINGSTEMA_SØKNAD;
import static org.assertj.core.api.Assertions.assertThat;

class MuligeManuelleBehandlingsendringerTest {
    private final LovligeSakKombinasjoner lovligeSakKombinasjoner = new LovligeSakKombinasjoner();
    private final FakeUnleash unleash = new FakeUnleash();

    @BeforeEach
    void init() {
        unleash.enableAll();
    }

    @Test
    void hentMuligeStatuser_temaOvrigeSedMed_avsluttetErMulig() {
        var muligeStatuser = MuligeManuelleBehandlingsendringer.hentMuligeStatuser(behandlingMedTema(Behandlingstema.ØVRIGE_SED_MED));
        assertThat(muligeStatuser).containsExactlyInAnyOrder(AVVENT_DOK_PART, AVVENT_DOK_UTL, UNDER_BEHANDLING, AVVENT_FAGLIG_AVKLARING, AVSLUTTET);
    }

    @Test
    void hentMuligeStatuser_temaArbeidUtland_avsluttetErIkkeMulig() {
        var muligeStatuser = MuligeManuelleBehandlingsendringer.hentMuligeStatuser(behandlingMedTema(Behandlingstema.ARBEID_I_UTLANDET));
        assertThat(muligeStatuser).containsExactlyInAnyOrder(AVVENT_DOK_PART, AVVENT_DOK_UTL, UNDER_BEHANDLING, AVVENT_FAGLIG_AVKLARING);
    }

    @Test
    void hentMuligeBehandlingstema_typeEndretPeriode_returnererUtsendt() {
        var muligeBehandlingstema = MuligeManuelleBehandlingsendringer.hentMuligeBehandlingstema(behandlingMedTemaOgType(UTSENDT_ARBEIDSTAKER, ENDRET_PERIODE), behandlingsresultatSendtUtland(false), false);
        assertThat(muligeBehandlingstema).containsExactly(UTSENDT_SELVSTENDIG);
    }

    @Test
    void hentMuligeBehandlingstema_gyldigSøknadBehandlingstemaToggleAv_returnererSøknadBehandlingstema() {
        var muligeBehandlingstema = MuligeManuelleBehandlingsendringer.hentMuligeBehandlingstema(behandlingMedTema(ARBEID_FLERE_LAND), behandlingsresultatSendtUtland(false), false);
        var behandlingstemaSøknadUtenValgtTema = BEHANDLINGSTEMA_SØKNAD.stream()
            .filter(tema -> tema != ARBEID_FLERE_LAND)
            .filter(tema -> tema != ARBEID_KUN_NORGE)
            .filter(tema -> tema != ARBEID_TJENESTEPERSON_ELLER_FLY)
            .collect(Collectors.toSet());
        assertThat(muligeBehandlingstema).isEqualTo(behandlingstemaSøknadUtenValgtTema);
    }

    @Test
    void hentMuligeBehandlingstema_gyldigSøknadBehandlingstemaTogglePå_returnererSøknadBehandlingstema() {
        var muligeBehandlingstema = MuligeManuelleBehandlingsendringer.hentMuligeBehandlingstema(behandlingMedTema(ARBEID_FLERE_LAND), behandlingsresultatSendtUtland(false), true);
        var behandlingstemaSøknadUtenValgtTema = BEHANDLINGSTEMA_SØKNAD.stream().filter(tema -> tema != ARBEID_FLERE_LAND).collect(Collectors.toSet());
        assertThat(muligeBehandlingstema).isEqualTo(behandlingstemaSøknadUtenValgtTema);
    }

    @Test
    void hentMuligeBehandlingstema_gyldigSEDForespørselBehandlingstema_returnererSEDForespørselBehandlingstema() {
        var muligeBehandlingstema = MuligeManuelleBehandlingsendringer.hentMuligeBehandlingstema(behandlingMedTema(ØVRIGE_SED_MED), behandlingsresultatSendtUtland(false), false);
        var behandlingstemaSedForespørselUtenValgtTema = BEHANDLINGSTEMA_SED_FORESPØRSEL.stream().filter(tema -> tema != ØVRIGE_SED_MED).collect(Collectors.toSet());
        assertThat(muligeBehandlingstema).isEqualTo(behandlingstemaSedForespørselUtenValgtTema);
    }

    @Test
    void hentMuligeBehandlingstema_ugyldigBehandlingstema_returnererTomListe() {
        var muligeBehandlingstema = MuligeManuelleBehandlingsendringer.hentMuligeBehandlingstema(behandlingMedTema(REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING), behandlingsresultatSendtUtland(false), false);
        assertThat(muligeBehandlingstema).isEmpty();
    }

    @Test
    void hentMuligeBehandlingstema_inaktivBehandling_returnererTomListe() {
        var muligeBehandlingstema = MuligeManuelleBehandlingsendringer.hentMuligeBehandlingstema(avsluttetBehandlingMedTema(ARBEID_FLERE_LAND), behandlingsresultatSendtUtland(false), false);
        assertThat(muligeBehandlingstema).isEmpty();
    }

    @Test
    void hentMuligeBehandlingstema_erArtikkel16MedSendtAnmodningOmUnntak_returnererTomListe() {
        var muligeBehandlingstema = MuligeManuelleBehandlingsendringer.hentMuligeBehandlingstema(behandlingMedTema(ARBEID_FLERE_LAND), behandlingsresultatSendtUtland(true), false);
        assertThat(muligeBehandlingstema).isEmpty();
    }

    @Test
    void hentMuligeTyper_temaForespørselTrygdemyndighet_returnererHenvendelse() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(EU_EOS, UNNTAK);
        Behandling behandling = behandlingMedTemaOgType(FORESPØRSEL_TRYGDEMYNDIGHET, HENVENDELSE);
        behandling.setFagsak(fagsak);
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper_NY(behandling);
        assertThat(muligeTyper).containsExactly(HENVENDELSE);
    }

    @Test
    void hentMuligeTyper_temaArbeidThenestepersonEllerFly_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(EU_EOS, MEDLEMSKAP_LOVVALG);
        Behandling behandling = behandlingMedTemaOgType(ARBEID_TJENESTEPERSON_ELLER_FLY, NY_VURDERING);
        behandling.setFagsak(fagsak);
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper_NY(behandling);
        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeTyper_EU_EOS_MEDLEMSKAP_LOVVALG_temaIkkeYrkesAktiv_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(EU_EOS, MEDLEMSKAP_LOVVALG);
        Behandling behandling = behandlingMedTemaOgType(IKKE_YRKESAKTIV, NY_VURDERING);
        behandling.setFagsak(fagsak);
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper_NY(behandling);
        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeTyper_EU_EOS_UNNTAK_temaForespørselTrygdemyndighet_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(EU_EOS, UNNTAK);
        Behandling behandling = behandlingMedTemaOgType(FORESPØRSEL_TRYGDEMYNDIGHET, HENVENDELSE);
        behandling.setFagsak(fagsak);
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper_NY(behandling);
        assertThat(muligeTyper).contains(HENVENDELSE);
    }

    @Test
    void hentMuligeTyper_EU_EOS_TRYGDEAVGIFT_temaYrkesaktiv_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(EU_EOS, TRYGDEAVGIFT);
        Behandling behandling = behandlingMedTemaOgType(YRKESAKTIV, KLAGE);
        behandling.setFagsak(fagsak);
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper_NY(behandling);
        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeTyper_FTRL_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(FTRL, MEDLEMSKAP_LOVVALG);
        Behandling behandling = behandlingMedTemaOgType(YRKESAKTIV, KLAGE);
        behandling.setFagsak(fagsak);
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper_NY(behandling);
        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeTyper_FTRL_LOVVALG_MEDLEMSKAP_temaPensjonist_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(FTRL, MEDLEMSKAP_LOVVALG);
        Behandling behandling = behandlingMedTemaOgType(PENSJONIST, KLAGE);
        behandling.setFagsak(fagsak);
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper_NY(behandling);
        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeTyper_FTRL_TRYGDEAVGIFT_temaPensjonist_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(FTRL, TRYGDEAVGIFT);
        Behandling behandling = behandlingMedTemaOgType(PENSJONIST, KLAGE);
        behandling.setFagsak(fagsak);
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper_NY(behandling);
        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeTyper_FTRL_TRYGDEAVGIFT_temaYrkesaktiv_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(FTRL, TRYGDEAVGIFT);
        Behandling behandling = behandlingMedTemaOgType(YRKESAKTIV, KLAGE);
        behandling.setFagsak(fagsak);
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper_NY(behandling);
        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeTyper_TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_temaYrkesaktiv_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(TRYGDEAVTALE, MEDLEMSKAP_LOVVALG);
        Behandling behandling = behandlingMedTemaOgType(YRKESAKTIV, KLAGE);
        behandling.setFagsak(fagsak);
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper_NY(behandling);
        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeTyper_TRYGDEAVTALE_LOVVALG_MEDLEMSKAP_temaPensjonist_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(TRYGDEAVTALE, MEDLEMSKAP_LOVVALG);
        Behandling behandling = behandlingMedTemaOgType(PENSJONIST, KLAGE);
        behandling.setFagsak(fagsak);
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper_NY(behandling);
        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeTyper_TRYGDEAVTALE_UNNTAK_temaForespørselTrygdemyndighet_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(TRYGDEAVTALE, UNNTAK);
        Behandling behandling = behandlingMedTemaOgType(FORESPØRSEL_TRYGDEMYNDIGHET, HENVENDELSE);
        behandling.setFagsak(fagsak);
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper_NY(behandling);
        assertThat(muligeTyper).contains(HENVENDELSE);
    }

    @Test
    void hentMuligeTyper_TRYGDEAVTALE_TRYGDEAVGIFT_temaYrkesaktiv_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(TRYGDEAVTALE, TRYGDEAVGIFT);
        Behandling behandling = behandlingMedTemaOgType(YRKESAKTIV, KLAGE);
        behandling.setFagsak(fagsak);
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper_NY(behandling);
        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeTyper_TRYGDEAVTALE_TRYGDEAVGIFT_temaPensjonist_returnererLovligKombinasjon() {
        Fagsak fagsak = fagsakMedSakstypeOgSakstema(TRYGDEAVTALE, TRYGDEAVGIFT);
        Behandling behandling = behandlingMedTemaOgType(PENSJONIST, KLAGE);
        behandling.setFagsak(fagsak);
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper_NY(behandling);
        assertThat(muligeTyper).contains(NY_VURDERING, FØRSTEGANG, HENVENDELSE, KLAGE);
    }

    @Test
    void hentMuligeTyper_feilType_returnererTomListe() {
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper(behandlingMedTemaOgType(UTSENDT_ARBEIDSTAKER, SOEKNAD));
        assertThat(muligeTyper).isEmpty();
    }

    @Test
    void hentMuligeTyper_feilTema_returnererTomListe() {
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper(behandlingMedTema(ARBEID_FLERE_LAND));
        assertThat(muligeTyper).isEmpty();
    }

    @Test
    void hentMuligeTyper_inaktivBehandling_returnererTomListe() {
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper(avsluttetBehandlingMedTema(UTSENDT_ARBEIDSTAKER));
        assertThat(muligeTyper).isEmpty();
    }

    private Behandling behandlingMedTema(Behandlingstema tema) {
        var behandling = new Behandling();
        behandling.setTema(tema);
        return behandling;
    }

    private Behandling avsluttetBehandlingMedTema(Behandlingstema tema) {
        var behandling = behandlingMedTema(tema);
        behandling.setStatus(AVSLUTTET);
        return behandling;
    }

    private Behandling behandlingMedTemaOgType(Behandlingstema tema, Behandlingstyper type) {
        var behandling = behandlingMedTema(tema);
        behandling.setType(type);
        return behandling;
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

    private Behandlingsresultat behandlingsresultatSendtUtland(boolean sendtUtland) {
        var behandlingsresultat = new Behandlingsresultat();
        var anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setSendtUtland(sendtUtland);
        behandlingsresultat.setAnmodningsperioder(Set.of(anmodningsperiode));
        return behandlingsresultat;
    }
}
