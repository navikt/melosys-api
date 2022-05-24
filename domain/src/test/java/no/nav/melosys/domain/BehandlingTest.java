package no.nav.melosys.domain;

import java.util.List;
import java.util.Set;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingstema;
import org.junit.jupiter.api.Test;

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
