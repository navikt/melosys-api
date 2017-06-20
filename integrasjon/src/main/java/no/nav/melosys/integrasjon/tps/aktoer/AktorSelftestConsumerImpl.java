package no.nav.melosys.integrasjon.tps.aktoer;

import no.nav.tjeneste.virksomhet.aktoer.v2.binding.AktoerV2;

class AktorSelftestConsumerImpl implements AktorSelftestConsumer {
    private AktoerV2 port;
    private String endpointUrl;

    public AktorSelftestConsumerImpl(AktoerV2 port, String endpointUrl) {
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
