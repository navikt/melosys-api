package no.nav.melosys.integrasjon.sakogbehandling;

public interface SakOgBehandlingClient {

    void sendBehandlingOpprettet(BehandlingStatusMapper mapper);

    void sendBehandlingAvsluttet(BehandlingStatusMapper mapper);
}
