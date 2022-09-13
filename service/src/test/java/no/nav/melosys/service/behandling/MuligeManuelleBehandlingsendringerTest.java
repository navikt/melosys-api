package no.nav.melosys.service.behandling;

import java.util.Set;
import java.util.stream.Collectors;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.domain.Behandling.BEHANDLINGSTEMA_SED_FORESPØRSEL;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*;
import static no.nav.melosys.service.behandling.MuligeManuelleBehandlingsendringer.BEHANDLINGSTEMA_SØKNAD;
import static org.assertj.core.api.Assertions.assertThat;

class MuligeManuelleBehandlingsendringerTest {


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
    void hentMuligeTyper_temaEndretPeriode_returnererNyVurdering() {
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper(behandlingMedTemaOgType(UTSENDT_ARBEIDSTAKER, ENDRET_PERIODE));
        assertThat(muligeTyper).containsExactly(NY_VURDERING);
    }

    @Test
    void hentMuligeTyper_temaNyVurdering_returnererEndretPeriode() {
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper(behandlingMedTemaOgType(UTSENDT_SELVSTENDIG, NY_VURDERING));
        assertThat(muligeTyper).containsExactly(ENDRET_PERIODE);
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

    private Behandlingsresultat behandlingsresultatSendtUtland(boolean sendtUtland) {
        var behandlingsresultat = new Behandlingsresultat();
        var anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setSendtUtland(sendtUtland);
        behandlingsresultat.setAnmodningsperioder(Set.of(anmodningsperiode));
        return behandlingsresultat;
    }
}
