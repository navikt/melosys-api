package no.nav.melosys.integrasjon.medl.behandle;

import no.nav.tjeneste.virksomhet.behandlemedlemskap.v2.BehandleMedlemskapV2;

public class BehandleMedlemskapSelftestConsumerImpl implements BehandleMedlemskapSelftestConsumer {

    private BehandleMedlemskapV2 port;
    private String endpointUrl;

    public BehandleMedlemskapSelftestConsumerImpl(BehandleMedlemskapV2 port, String endpointUrl) {
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
