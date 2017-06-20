package no.nav.melosys.integrasjon.gsak.behandlesak;

import no.nav.tjeneste.virksomhet.behandlesak.v1.binding.BehandleSakV1;

class BehandleSakSelftestConsumerImpl implements BehandleSakSelftestConsumer {
    private BehandleSakV1 port;
    private String endpointUrl;

    public BehandleSakSelftestConsumerImpl(BehandleSakV1 port, String endpointUrl) {
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
