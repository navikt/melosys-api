package no.nav.melosys.domain;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class BehandlingTest {

    @Test
    public void erAktiv_underBehandling_ja() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.VURDER_DOKUMENT);
        assertThat(behandling.erAktiv()).isTrue();
    }

    @Test
    public void erAktiv_avsluttet_nei() {
        Behandling behandling = new Behandling();
        behandling.setStatus(Behandlingsstatus.AVSLUTTET);
        assertThat(behandling.erAktiv()).isFalse();
    }

    @Test
    void saksopplysningEksisterer_eksisterer_true() {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PDL_PERSOPL);

        Behandling behandling = new Behandling();
        behandling.setSaksopplysninger(Set.of(saksopplysning));

        assertThat(behandling.saksopplysningEksisterer(SaksopplysningType.PDL_PERSOPL)).isTrue();
    }

    @Test
    void saksopplysningEksisterer_eksistererIkke_false() {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PDL_PERSOPL);

        Behandling behandling = new Behandling();
        behandling.setSaksopplysninger(Set.of(saksopplysning));

        assertThat(behandling.saksopplysningEksisterer(SaksopplysningType.PDL_PERS_SAKS)).isFalse();
    }
}
