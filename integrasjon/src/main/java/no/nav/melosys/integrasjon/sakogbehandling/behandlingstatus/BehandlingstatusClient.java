package no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus;

public interface BehandlingstatusClient {

    void sendBehandlingOpprettet(BehandlingStatusMapper mapper);

    void sendBehandlingAvsluttet(BehandlingStatusMapper mapper);
}
