package no.nav.melosys.domain;

import no.nav.melosys.domain.behandling.Behandling;
import no.nav.melosys.domain.kodeverk.behandlinger.Behandlingsstatus;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BehandlingTest {

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
}