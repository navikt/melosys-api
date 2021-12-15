package no.nav.melosys.domain;

import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import org.junit.jupiter.api.Test;

import java.util.List;
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
    void saksopplysningerEksistererIkke_eksisterer_false() {
        Saksopplysning saksopplysning1 = new Saksopplysning();
        saksopplysning1.setType(SaksopplysningType.PERSHIST);
        Saksopplysning saksopplysning2 = new Saksopplysning();
        saksopplysning1.setType(SaksopplysningType.PERSOPL);

        Behandling behandling = new Behandling();
        behandling.setSaksopplysninger(Set.of(saksopplysning1, saksopplysning2));

        assertThat(behandling.saksopplysningerEksistererIkke(List.of(SaksopplysningType.PDL_PERSOPL, SaksopplysningType.PERSOPL))).isFalse();
    }

    @Test
    void saksopplysningerEksistererIkke_eksistererIkke_true() {
        Saksopplysning saksopplysning = new Saksopplysning();
        saksopplysning.setType(SaksopplysningType.PDL_PERSOPL);

        Behandling behandling = new Behandling();
        behandling.setSaksopplysninger(Set.of(saksopplysning));

        assertThat(behandling.saksopplysningerEksistererIkke(List.of(SaksopplysningType.PDL_PERS_SAKS, SaksopplysningType.PERSHIST))).isTrue();
    }
}
