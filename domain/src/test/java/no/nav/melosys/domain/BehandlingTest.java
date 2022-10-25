package no.nav.melosys.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import org.junit.jupiter.api.Test;

import static no.nav.melosys.domain.kodeverk.Sakstemaer.MEDLEMSKAP_LOVVALG;
import static no.nav.melosys.domain.kodeverk.Sakstemaer.UNNTAK;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema.*;
import static no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstyper.*;
import static org.assertj.core.api.Assertions.assertThat;

class BehandlingTest {

    @Test
    void erAktiv_underBehandling_ja() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        assertThat(behandling.erAktiv()).isTrue();
    }

    @Test
    void erAktiv_avsluttet_nei() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        assertThat(behandling.erAktiv()).isFalse();
    }

    @Test
    void erRedigerbar_erUnderBehandling_ja() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.UNDER_BEHANDLING);
        assertThat(behandling.erRedigerbar()).isTrue();
    }

    @Test
    void erRedigerbar_erAvsluttet_nei() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        assertThat(behandling.erRedigerbar()).isFalse();
    }

    @Test
    void erRedigerbar_erMidlertidigLovvalgsbeslutning_nei() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.MIDLERTIDIG_LOVVALGSBESLUTNING);
        assertThat(behandling.erRedigerbar()).isFalse();
    }

    @Test
    void erRedigerbar_erIverksetterVedtak_nei() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.IVERKSETTER_VEDTAK);
        assertThat(behandling.erRedigerbar()).isFalse();
    }
    @Test
    void erRedigerbar_erAnmodningOmUnntakSendt_nei() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        assertThat(behandling.erRedigerbar()).isFalse();
    }

    @Test
    void erRedigerbar_erAnmodningOmUnntakSendtIkkeYrkesaktiv_ja() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.ANMODNING_UNNTAK_SENDT);
        behandling.setTema(Behandlingstema.IKKE_YRKESAKTIV);
        assertThat(behandling.erRedigerbar()).isTrue();
    }

    @Test
    void utledFristForBehandling_8Uker() {
        LocalDate behandlingsfrist = Behandling.utledFristForBehandling(MEDLEMSKAP_LOVVALG, BESLUTNING_LOVVALG_ANNET_LAND, FØRSTEGANG);

        assertThat(behandlingsfrist).isEqualTo(LocalDate.now().plusWeeks(8));
    }

    @Test
    void utledFristForBehandling_70dager() {
        LocalDate behandlingsfrist = Behandling.utledFristForBehandling(MEDLEMSKAP_LOVVALG, BESLUTNING_LOVVALG_ANNET_LAND, KLAGE);

        assertThat(behandlingsfrist).isEqualTo(LocalDate.now().plusDays(70));
    }

    @Test
    void utledFristForBehandling_90dager() {
        LocalDate behandlingsfrist_soknadsbehandlinger = Behandling.utledFristForBehandling(MEDLEMSKAP_LOVVALG, UTSENDT_ARBEIDSTAKER, FØRSTEGANG);
        LocalDate behandlingsfrist_anmodning_unntak = Behandling.utledFristForBehandling(MEDLEMSKAP_LOVVALG, ANMODNING_OM_UNNTAK_HOVEDREGEL, FØRSTEGANG);
        LocalDate behandlingsfrist_attester_fra_andre_trygdeavtaleland = Behandling.utledFristForBehandling(MEDLEMSKAP_LOVVALG, REGISTRERING_UNNTAK, FØRSTEGANG);
        LocalDate behandlingsfrist_henvendelser = Behandling.utledFristForBehandling(MEDLEMSKAP_LOVVALG, UTSENDT_ARBEIDSTAKER, HENVENDELSE);

        assertThat(behandlingsfrist_soknadsbehandlinger).isEqualTo(LocalDate.now().plusDays(90));
        assertThat(behandlingsfrist_anmodning_unntak).isEqualTo(LocalDate.now().plusDays(90));
        assertThat(behandlingsfrist_attester_fra_andre_trygdeavtaleland).isEqualTo(LocalDate.now().plusDays(90));
        assertThat(behandlingsfrist_henvendelser).isEqualTo(LocalDate.now().plusDays(90));
    }

    @Test
    void utledFristForBehandling_180dager() {
        LocalDate behandlingsfrist = Behandling.utledFristForBehandling(UNNTAK, REGISTRERING_UNNTAK_NORSK_TRYGD_UTSTASJONERING, NY_VURDERING);
        LocalDate behandlingsfrist_ovrige = Behandling.utledFristForBehandling(UNNTAK, REGISTRERING_UNNTAK_NORSK_TRYGD_ØVRIGE, NY_VURDERING);

        assertThat(behandlingsfrist).isEqualTo(LocalDate.now().plusDays(180));
        assertThat(behandlingsfrist_ovrige).isEqualTo(LocalDate.now().plusDays(180));
    }

    @Test
    void saksopplysningerEksistererIkke_eksisterer_false() {
        Saksopplysning saksopplysning1 = new Saksopplysning();
        saksopplysning1.setType(SaksopplysningType.PERSHIST);
        Saksopplysning saksopplysning2 = new Saksopplysning();
        saksopplysning1.setType(SaksopplysningType.PERSOPL);

        Behandling behandling = new Behandling();
        behandling.setSaksopplysninger(Set.of(saksopplysning1, saksopplysning2));

        assertThat(behandling.manglerSaksopplysningerAvType(List.of(SaksopplysningType.PDL_PERSOPL, SaksopplysningType.PERSOPL))).isFalse();
    }

    @Test
    void saksopplysningerEksistererIkke_eksistererIkke_true() {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PDL_PERSOPL);

        Behandling behandling = new Behandling();
        behandling.setSaksopplysninger(Set.of(saksopplysning));

        assertThat(behandling.manglerSaksopplysningerAvType(List.of(SaksopplysningType.PDL_PERS_SAKS, SaksopplysningType.PERSHIST))).isTrue();
    }
}
