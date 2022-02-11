package no.nav.melosys.service.behandling;

import java.util.Set;

import no.nav.melosys.domain.Anmodningsperiode;
import no.nav.melosys.domain.Behandling;
import no.nav.melosys.domain.Behandlingsresultat;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.domain.Behandling.BEHANDLINGSTEMA_SED_FORESPØRSEL;
import static no.nav.melosys.domain.Behandling.BEHANDLINGSTEMA_SØKNAD;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus.*;
import static org.assertj.core.api.Assertions.assertThat;

public class MuligeBehandlingsverdierTest {


    @Test
    void hentMuligeStatuser_temaOvrigeSedMed_avsluttetErMulig() {
        var muligeStatuser = MuligeBehandlingsverdier.hentMuligeStatuser(behandlingMedTema(Behandlingstema.ØVRIGE_SED_MED));
        assertThat(muligeStatuser).containsExactlyInAnyOrder(AVVENT_DOK_PART, AVVENT_DOK_UTL, UNDER_BEHANDLING, AVVENT_FAGLIG_AVKLARING, AVSLUTTET);
    }

    @Test
    void hentMuligeStatuser_temaArbeidUtland_avsluttetErIkkeMulig() {
        var muligeStatuser = MuligeBehandlingsverdier.hentMuligeStatuser(behandlingMedTema(Behandlingstema.ARBEID_I_UTLANDET));
        assertThat(muligeStatuser).containsExactlyInAnyOrder(AVVENT_DOK_PART, AVVENT_DOK_UTL, UNDER_BEHANDLING, AVVENT_FAGLIG_AVKLARING);
    }

    @Test
    void hentMuligeBehandlingstema_gyldigSøknadBehandlingstema_returnererSøknadBehandlingstema() {
        var muligeBehandlingstema = MuligeBehandlingsverdier.hentMuligeBehandlingstema(behandlingMedTema(Behandlingstema.ARBEID_FLERE_LAND), behandlingsresultatSendtUtland(false));
        assertThat(muligeBehandlingstema).isEqualTo(BEHANDLINGSTEMA_SØKNAD);
    }

    @Test
    void hentMuligeBehandlingstema_gyldigSEDForespørselBehandlingstema_returnererSEDForespørselBehandlingstema() {
        var muligeBehandlingstema = MuligeBehandlingsverdier.hentMuligeBehandlingstema(behandlingMedTema(Behandlingstema.ØVRIGE_SED_MED), behandlingsresultatSendtUtland(false));
        assertThat(muligeBehandlingstema).isEqualTo(BEHANDLINGSTEMA_SED_FORESPØRSEL);
    }

    @Test
    void hentMuligeBehandlingstema_ugyldigBehandlingstema_returnererTomListe() {
        var muligeBehandlingstema = MuligeBehandlingsverdier.hentMuligeBehandlingstema(behandlingMedTema(Behandlingstema.REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING), behandlingsresultatSendtUtland(false));
        assertThat(muligeBehandlingstema).isEmpty();
    }

    @Test
    void hentMuligeBehandlingstema_inaktivBehandling_returnererTomListe() {
        var muligeBehandlingstema = MuligeBehandlingsverdier.hentMuligeBehandlingstema(avsluttetBehandlingMedTema(Behandlingstema.ARBEID_FLERE_LAND), behandlingsresultatSendtUtland(false));
        assertThat(muligeBehandlingstema).isEmpty();
    }

    @Test
    void hentMuligeBehandlingstema_erArtikkel16MedSendtAnmodningOmUnntak_returnererTomListe() {
        var muligeBehandlingstema = MuligeBehandlingsverdier.hentMuligeBehandlingstema(behandlingMedTema(Behandlingstema.ARBEID_FLERE_LAND), behandlingsresultatSendtUtland(true));
        assertThat(muligeBehandlingstema).isEmpty();
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

    private Behandlingsresultat behandlingsresultatSendtUtland(boolean sendtUtland) {
        var behandlingsresultat = new Behandlingsresultat();
        var anmodningsperiode = new Anmodningsperiode();
        anmodningsperiode.setSendtUtland(sendtUtland);
        behandlingsresultat.setAnmodningsperioder(Set.of(anmodningsperiode));
        return behandlingsresultat;
    }
}
