package no.nav.melosys.integrasjon.sakogbehandling.behandlingskjede;

import no.nav.tjeneste.virksomhet.sakogbehandling.v1.binding.SakOgBehandlingV1;

public class BehandlingskjedeSelftestConsumerImpl implements BehandlingskjedeSelftestConsumer {

    private SakOgBehandlingV1 port;
    private String endpointUrl;

    public BehandlingskjedeSelftestConsumerImpl(SakOgBehandlingV1 port, String endpointUrl) {
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
