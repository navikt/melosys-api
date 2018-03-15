package no.nav.melosys.integrasjon.gsak.behandleoppgave;

import no.nav.tjeneste.virksomhet.behandleoppgave.v1.BehandleOppgaveV1;

public class BehandleOppgaveSelftestConsumerImpl implements BehandleOppgaveSelftestConsumer {

    private BehandleOppgaveV1 port;

    private String endpointUrl;

    public BehandleOppgaveSelftestConsumerImpl(BehandleOppgaveV1 port, String endpointUrl) {
        this.port = port;
        this.endpointUrl = endpointUrl;
    }

    @Override
    public void ping() {
        port.ping();
    }

    @Override
    public String getEndpointUrl() {
        return endpointUrl;
    }
}
