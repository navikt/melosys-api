package no.nav.melosys.domain;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FagsakTest {

    @Test
    public void getAktivBehandling() {
        Fagsak fagsak = new Fagsak();
        Behandling b1 = new Behandling();
        b1.setStatus(BehandlingStatus.AVSLUTTET);

        Behandling b2 = new Behandling();
        b2.setStatus(BehandlingStatus.FORELØPIG);

        Behandling b3 = new Behandling();
        b3.setStatus(BehandlingStatus.AVSLUTTET);

        List<Behandling> behandlinger = new ArrayList<>();
        behandlinger.add(b1);
        behandlinger.add(b2);
        behandlinger.add(b3);
        fagsak.setBehandlinger(behandlinger);

        Behandling aktivBehandling = fagsak.getAktivBehandling();

        assertThat(aktivBehandling).isEqualTo(b2);
    }

    @Test
    public void getAktivBehandling_ingenAktive() {
        Fagsak fagsak = new Fagsak();
        Behandling b1 = new Behandling();
        b1.setStatus(BehandlingStatus.AVSLUTTET);

        Behandling b2 = new Behandling();
        b2.setStatus(BehandlingStatus.AVSLUTTET);

        List<Behandling> behandlinger = new ArrayList<>();
        behandlinger.add(b1);
        behandlinger.add(b2);
        fagsak.setBehandlinger(behandlinger);

        Behandling aktivBehandling = fagsak.getAktivBehandling();

        assertThat(aktivBehandling).isNull();
    }

    @Test(expected = RuntimeException.class)
    public void getAktivBehandling_feilTilstand() {
        Fagsak fagsak = new Fagsak();
        Behandling b1 = new Behandling();
        b1.setStatus(BehandlingStatus.FORELØPIG);

        Behandling b2 = new Behandling();
        b2.setStatus(BehandlingStatus.UNDER_BEHANDLING);

        List<Behandling> behandlinger = new ArrayList<>();
        behandlinger.add(b1);
        behandlinger.add(b2);
        fagsak.setBehandlinger(behandlinger);

        fagsak.getAktivBehandling();
    }
}