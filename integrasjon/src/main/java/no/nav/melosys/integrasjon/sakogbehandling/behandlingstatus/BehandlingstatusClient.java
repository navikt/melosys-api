package no.nav.melosys.integrasjon.sakogbehandling.behandlingstatus;

import no.nav.melosys.exception.IntegrasjonException;

public interface BehandlingstatusClient {

    void sendBehandlingOpprettet(BehandlingStatusMapper mapper) throws IntegrasjonException;

    void sendBehandlingAvsluttet(BehandlingStatusMapper mapper) throws IntegrasjonException;
}
