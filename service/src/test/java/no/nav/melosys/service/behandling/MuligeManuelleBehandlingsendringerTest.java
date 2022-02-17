package no.nav.melosys.service.behandling;

import java.util.Set;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.domain.Behandling.BEHANDLINGSTEMA_SED_FORESPØRSEL;
import static no.nav.melosys.domain.Behandling.BEHANDLINGSTEMA_SØKNAD;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.*;
import static org.assertj.core.api.Assertions.assertThat;

public class MuligeManuelleBehandlingsendringerTest {


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
    void hentMuligeBehandlingstema_gyldigSøknadBehandlingstema_returnererSøknadBehandlingstema() {
        var muligeBehandlingstema = MuligeManuelleBehandlingsendringer.hentMuligeBehandlingstema(behandlingMedTema(Behandlingstema.ARBEID_FLERE_LAND), behandlingsresultatSendtUtland(false));
        assertThat(muligeBehandlingstema).isEqualTo(BEHANDLINGSTEMA_SØKNAD);
    }

    @Test
    void hentMuligeBehandlingstema_gyldigSEDForespørselBehandlingstema_returnererSEDForespørselBehandlingstema() {
        var muligeBehandlingstema = MuligeManuelleBehandlingsendringer.hentMuligeBehandlingstema(behandlingMedTema(Behandlingstema.ØVRIGE_SED_MED), behandlingsresultatSendtUtland(false));
        assertThat(muligeBehandlingstema).isEqualTo(BEHANDLINGSTEMA_SED_FORESPØRSEL);
    }

    @Test
    void hentMuligeBehandlingstema_ugyldigBehandlingstema_returnererTomListe() {
        var muligeBehandlingstema = MuligeManuelleBehandlingsendringer.hentMuligeBehandlingstema(behandlingMedTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING), behandlingsresultatSendtUtland(false));
        assertThat(muligeBehandlingstema).isEmpty();
    }

    @Test
    void hentMuligeBehandlingstema_inaktivBehandling_returnererTomListe() {
        var muligeBehandlingstema = MuligeManuelleBehandlingsendringer.hentMuligeBehandlingstema(avsluttetBehandlingMedTema(Behandlingstema.ARBEID_FLERE_LAND), behandlingsresultatSendtUtland(false));
        assertThat(muligeBehandlingstema).isEmpty();
    }

    @Test
    void hentMuligeBehandlingstema_erArtikkel16MedSendtAnmodningOmUnntak_returnererTomListe() {
        var muligeBehandlingstema = MuligeManuelleBehandlingsendringer.hentMuligeBehandlingstema(behandlingMedTema(Behandlingstema.ARBEID_FLERE_LAND), behandlingsresultatSendtUtland(true));
        assertThat(muligeBehandlingstema).isEmpty();
    }

    @Test
    void hentMuligeTyper_temaEndretPeriode_returnererNyVurdering() {
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper(behandlingMedTemaOgType(Behandlingstema.UTSENDT_ARBEIDSTAKER, Behandlingstyper.ENDRET_PERIODE));
        assertThat(muligeTyper).containsExactly(Behandlingstyper.NY_VURDERING);
    }

    @Test
    void hentMuligeTyper_temaNyVurdering_returnererEndretPeriode() {
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper(behandlingMedTemaOgType(Behandlingstema.UTSENDT_SELVSTENDIG, Behandlingstyper.NY_VURDERING));
        assertThat(muligeTyper).containsExactly(Behandlingstyper.ENDRET_PERIODE);
    }

    @Test
    void hentMuligeTyper_feilType_returnererTomListe() {
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper(behandlingMedTemaOgType(Behandlingstema.UTSENDT_ARBEIDSTAKER, Behandlingstyper.SOEKNAD));
        assertThat(muligeTyper).isEmpty();
    }

    @Test
    void hentMuligeTyper_feilTema_returnererTomListe() {
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper(behandlingMedTema(Behandlingstema.ARBEID_FLERE_LAND));
        assertThat(muligeTyper).isEmpty();
    }

    @Test
    void hentMuligeTyper_inaktivBehandling_returnererTomListe() {
        var muligeTyper = MuligeManuelleBehandlingsendringer.hentMuligeTyper(avsluttetBehandlingMedTema(Behandlingstema.UTSENDT_ARBEIDSTAKER));
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
