package no.nav.melosys.integrasjon.sakogbehandling;

import no.nav.melosys.exception.IntegrasjonException;

public interface SakOgBehandlingClient {

    void sendBehandlingOpprettet(BehandlingStatusMapper mapper) throws IntegrasjonException;

    void sendBehandlingAvsluttet(BehandlingStatusMapper mapper) throws IntegrasjonException;
}
